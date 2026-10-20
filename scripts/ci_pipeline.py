import argparse
import json
import os
from pathlib import Path
import re
import subprocess
from xml.etree import ElementTree
from xml.parsers import expat


REPOSITORY = "ShiftWise-AI/kubeoptix-configurations"
OPERATOR = "ShiftWise-AI/kubeoptix-operator"
IMAGE = "quay.io/parraes/kubeoptix-configurations"
SEMVER = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)")


def run(*args, cwd=None):
    return subprocess.check_output(args, cwd=cwd, text=True).strip()


def api(path, method="GET", payload=None):
    command = ["gh", "api", path, "--method", method]
    if payload is not None:
        command += ["--input", "-"]
    return json.loads(subprocess.check_output(
        command, input=None if payload is None else json.dumps(payload), text=True
    ))


def allowed_pr(pr, repository=REPOSITORY):
    base = pr.get("base", {})
    head = pr.get("head", {})
    if (base.get("repo") or {}).get("full_name") != repository:
        return False
    if (head.get("repo") or {}).get("full_name") != repository:
        return False
    source = head.get("ref", "")
    if base.get("ref") == "develop":
        return bool(re.fullmatch(r"(?:feature|fix)/[^\s]+", source))
    if base.get("ref") == "main":
        return source == "develop" or bool(re.fullmatch(r"fix/[^\s]+", source))
    return False


def version_tuple(value):
    if not SEMVER.fullmatch(value):
        raise ValueError(f"Invalid MAJOR.MINOR.PATCH: {value}")
    return tuple(map(int, value.split(".")))


def next_version(current, released):
    baseline = version_tuple(current)
    versions = [version_tuple(value) for value in released]
    if any(value[:2] > baseline[:2] for value in versions):
        raise ValueError("Source major/minor is older than an existing release")
    patch = max([baseline[2]] + [value[2] for value in versions if value[:2] == baseline[:2]])
    return f"{baseline[0]}.{baseline[1]}.{patch + 1}"


def project_version(text):
    root = ElementTree.fromstring(text)
    if root.tag.rsplit("}", 1)[-1] != "project":
        raise ValueError("Expected a Maven project POM")
    namespace = root.tag.partition("}")[0][1:] if root.tag.startswith("{") else ""
    tag = lambda name: f"{{{namespace}}}{name}" if namespace else name
    artifact = root.find(tag("artifactId"))
    version = root.find(tag("version"))
    if artifact is None or artifact.text != "kubeoptix-configurations" or version is None or not version.text:
        raise ValueError("Expected the kubeoptix-configurations project version")
    return version.text.strip()


def project_version_line(text):
    parser = expat.ParserCreate()
    stack = []
    result = {"line": None, "parts": []}

    def start(name, attributes):
        stack.append(name)
        if stack == ["project", "version"]:
            if result["line"] is not None:
                raise ValueError("Multiple project-level versions in pom.xml")
            result["line"] = parser.CurrentLineNumber

    def data(value):
        if stack == ["project", "version"]:
            result["parts"].append(value)

    def end(name):
        stack.pop()

    parser.StartElementHandler = start
    parser.CharacterDataHandler = data
    parser.EndElementHandler = end
    parser.Parse(text.encode("utf-8"), True)
    if result["line"] is None or "".join(result["parts"]).strip() != project_version(text):
        raise ValueError("Could not locate the project version in pom.xml")
    return result["line"]


def replace_openapi_version(text, current, version):
    version_tuple(current)
    version_tuple(version)
    updated, count = re.subn(
        rf"(?m)^(quarkus\.smallrye-openapi\.info-version=){re.escape(current)}$",
        rf"\g<1>{version}",
        text,
    )
    if count != 1:
        raise ValueError("Expected exactly one OpenAPI info-version matching the project version")
    return updated


def replace_version(text, version):
    old = project_version(text)
    version_tuple(old)
    version_tuple(version)
    lines = text.splitlines(keepends=True)
    index = project_version_line(text) - 1
    updated, count = re.subn(
        rf"^([ \t]*<version>){re.escape(old)}(</version>[ \t]*(?:\r?\n)?)$",
        rf"\g<1>{version}\g<2>",
        lines[index],
    )
    if count != 1:
        raise ValueError("Expected a single simple project version element")
    lines[index] = updated
    result = "".join(lines)
    if project_version(result) != version:
        raise ValueError("Updated Maven project version did not validate")
    return result


def verify_source(source):
    if not re.fullmatch(r"[0-9a-f]{40}", source):
        raise ValueError("Expected full source commit SHA")
    run("git", "merge-base", "--is-ancestor", source, "origin/main")
    prs = api(f"repos/{REPOSITORY}/commits/{source}/pulls?per_page=100")
    if not any(pr.get("merged_at") and pr.get("merge_commit_sha") == source
               and pr["base"]["ref"] == "main" and allowed_pr(pr) for pr in prs):
        raise ValueError("Release source must be the merge commit of an allowed same-repository PR to main")


def output(**values):
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
        for key, value in values.items():
            stream.write(f"{key}={value}\n")


def reserve(source):
    run("git", "fetch", "origin", "main", "--tags")
    verify_source(source)
    tags = run("git", "tag", "--list", "v*").splitlines()
    releases = [tag[1:] for tag in tags if SEMVER.fullmatch(tag[1:])]
    marker = f"Release-Source: {source}"
    matches = [tag for tag in tags if SEMVER.fullmatch(tag[1:]) and
               marker in run("git", "for-each-ref", "--format=%(contents)", f"refs/tags/{tag}").splitlines()]
    if len(matches) > 1:
        raise ValueError("Multiple releases reserved for the same source")
    if matches:
        tag = matches[0]
        pom = run("git", "show", f"{tag}:pom.xml")
        if project_version(pom) != tag[1:]:
            raise ValueError("Reserved release version does not match pom.xml")
        application = run("git", "show", f"{tag}:src/main/resources/application.properties")
        if f"quarkus.smallrye-openapi.info-version={tag[1:]}" not in application.splitlines():
            raise ValueError("Reserved release version does not match the OpenAPI info-version")
        if run("git", "rev-parse", f"{tag}^1") != source:
            raise ValueError("Reserved release has an unexpected source parent")
        changed = run("git", "diff-tree", "--no-commit-id", "--name-only", "-r", f"{tag}^1", tag).splitlines()
        if set(changed) != {"pom.xml", "src/main/resources/application.properties"}:
            raise ValueError("Reserved release commit contains unexpected file changes")
    else:
        run("git", "checkout", "--detach", source)
        path = Path("pom.xml")
        pom = path.read_bytes().decode("utf-8")
        current = project_version(pom)
        version = next_version(current, releases)
        path.write_bytes(replace_version(pom, version).encode("utf-8"))
        application_path = Path("src/main/resources/application.properties")
        application = application_path.read_bytes().decode("utf-8")
        application_path.write_bytes(replace_openapi_version(application, current, version).encode("utf-8"))
        run("git", "add", "pom.xml", str(application_path))
        changed = run("git", "diff", "--cached", "--name-only").splitlines()
        if set(changed) != {"pom.xml", "src/main/resources/application.properties"}:
            raise ValueError("Release reservation staged unexpected file changes")
        run("git", "commit", "-m", f"Release {version}\n\n{marker}")
        tag = f"v{version}"
        run("git", "tag", "-a", tag, "-m", marker)
        run("git", "push", "origin", f"refs/tags/{tag}")
    run("git", "checkout", "--detach", tag)
    output(version=tag[1:], tag=tag, source=source, commit=run("git", "rev-parse", "HEAD"))


def operator_image(text, reference):
    updated, count = re.subn(
        r'(?m)^([ \t]*ConfigurationsImage:[ \t]+)(?:constants\.ConfigurationsRegistry \+ "/" \+ constants\.ConfigurationsName \+ ":" \+ constants\.ImageTag|"[^"\n]+"),$',
        lambda match: f'{match[1]}"{reference}",',
        text,
    )
    if count != 1:
        raise ValueError("Operator ConfigurationsImage contract changed; refusing broad edits")
    return updated


def update_operator(version, digest, directory):
    version_tuple(version)
    if not re.fullmatch(r"sha256:[0-9a-f]{64}", digest):
        raise ValueError("Invalid registry digest")
    reference = f"{IMAGE}:{version}@{digest}"
    root = Path(directory).resolve()
    branch = f"fix/configurations-{version}"
    run("git", "fetch", "origin", "main", cwd=root)
    remote_branches = run("git", "ls-remote", "--heads", "origin", branch, cwd=root)
    if remote_branches:
        run("git", "fetch", "origin", branch, cwd=root)
        run("git", "checkout", "--detach", "FETCH_HEAD", cwd=root)
        changed_from_main = run("git", "diff", "--name-only", "origin/main...HEAD", cwd=root).splitlines()
        if changed_from_main not in ([], ["internal/operands/settings.go"]):
            raise ValueError("Unexpected files in the bot-owned Operator update branch")
    path = root / "internal/operands/settings.go"
    before = path.read_text(encoding="utf-8")
    after = operator_image(before, reference)
    if after != before:
        path.write_text(after, encoding="utf-8")
        run("gofmt", "-w", str(path), cwd=root)
    probe = root / "internal/operands/ci_release_test.go"
    if probe.exists():
        raise ValueError("Refusing to overwrite an existing Operator test")
    probe.write_text(
        'package operands\nimport "testing"\n'
        'func TestCIReleaseImage(t *testing.T) {\n'
        f'if got := FromCR(newShiftWise()).ConfigurationsImage; got != "{reference}" {{\n'
        't.Fatalf("unexpected Configurations image: %s", got)\n}\n}\n',
        encoding="utf-8",
    )
    try:
        run("go", "test", "./internal/operands", cwd=root)
    finally:
        probe.unlink()
    changed = run("git", "diff", "--name-only", cwd=root).splitlines()
    if changed not in ([], ["internal/operands/settings.go"]):
        raise ValueError("Unexpected Operator changes")
    if changed:
        run("git", "checkout", "-B", branch, cwd=root)
        run("git", "add", "internal/operands/settings.go", cwd=root)
        run("git", "commit", "-m", f"fix: pin Configurations to {version}", cwd=root)
        run("git", "push", "origin", f"HEAD:refs/heads/{branch}", cwd=root)
    existing = api(f"repos/{OPERATOR}/pulls?state=open&head=ShiftWise-AI:{branch}&base=main")
    if not changed and not existing and not remote_branches:
        return
    if not existing:
        api(f"repos/{OPERATOR}/pulls", "POST", {
            "title": f"fix: update Configurations to {version}",
            "head": branch,
            "base": "main",
            "body": f"Published and verified image: `{reference}`.\n\nOnly the Configurations image changes; Operator and other component versions remain unchanged.",
        })


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["reserve", "operator"])
    parser.add_argument("--source")
    parser.add_argument("--version")
    parser.add_argument("--digest")
    parser.add_argument("--directory", default="operator")
    args = parser.parse_args()
    if args.command == "reserve":
        reserve(args.source)
    else:
        update_operator(args.version, args.digest, args.directory)


if __name__ == "__main__":
    main()