import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import ci_pipeline as pipeline


POM = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.shiftwise.ai.kubeoptix</groupId>
    <artifactId>kubeoptix-configurations</artifactId>
    <version>1.0.0</version>
</project>
'''


class PipelineTests(unittest.TestCase):
    def test_workflow_contract_and_queue_configuration(self):
        root = Path(__file__).resolve().parents[1]
        release = (root / ".github/workflows/ci-release.yml").read_text()
        self.assertIn("queue: max", release)
        self.assertIn("cancel-in-progress: false", release)
        self.assertIn("needs: [ci-required]", release)
        self.assertIn("environment: quay-release", release)
        self.assertIn("permissions:\n  contents: read", release)
        governance = (root / ".github/workflows/governance.yml").read_text()
        self.assertIn("name: PR governance", governance)
        self.assertNotRegex(governance, r"(?m)^\s+run:")
        self.assertIn("actions/github-script@", governance)

    def test_branch_matrix_and_repository_identity(self):
        for base in ("develop", "main", "stage"):
            for source in ("feature/a", "fix/a", "develop", "main", "feature/", "fix/", "other/a"):
                for fork in (False, True):
                    with self.subTest(base=base, source=source, fork=fork):
                        repository = "attacker/fork" if fork else pipeline.REPOSITORY
                        pr = {
                            "base": {"ref": base, "repo": {"full_name": pipeline.REPOSITORY}},
                            "head": {"ref": source, "repo": {"full_name": repository}},
                        }
                        expected = not fork and (
                            base == "develop" and source in ("feature/a", "fix/a") or
                            base == "main" and source in ("develop", "fix/a")
                        )
                        self.assertEqual(pipeline.allowed_pr(pr), expected)
                        pr["base"]["repo"]["full_name"] = "attacker/fork"
                        self.assertFalse(pipeline.allowed_pr(pr))

    def test_patch_uses_existing_release_history(self):
        self.assertEqual(pipeline.next_version("1.0.0", ["1.0.2", "1.0.5"]), "1.0.6")
        self.assertEqual(pipeline.next_version("1.4.7", []), "1.4.8")
        with self.assertRaises(ValueError):
            pipeline.next_version("1.0.0", ["2.0.0"])

    def test_only_maven_project_version_changes(self):
        updated = pipeline.replace_version(POM, "1.0.6")
        self.assertEqual(updated, POM.replace("<version>1.0.0</version>", "<version>1.0.6</version>"))
        with self.assertRaises(ValueError):
            pipeline.replace_version(POM.replace("kubeoptix-configurations", "other"), "1.0.6")
        application = "quarkus.smallrye-openapi.info-version=1.0.0\nquarkus.http.port=8000\n"
        self.assertEqual(
            pipeline.replace_openapi_version(application, "1.0.0", "1.0.6"),
            "quarkus.smallrye-openapi.info-version=1.0.6\nquarkus.http.port=8000\n",
        )
        with self.assertRaises(ValueError):
            pipeline.replace_openapi_version(application, "1.0.1", "1.0.6")

    def test_operator_update_is_scoped_and_idempotent(self):
        original = (
            'ConfigurationsImage: constants.ConfigurationsRegistry + "/" + constants.ConfigurationsName + ":" + constants.ImageTag,\n'
            'ReporterImage: registryImage(constants.ReporterName),\n'
        )
        reference = pipeline.IMAGE + ":1.0.6@sha256:" + "a" * 64
        result = pipeline.operator_image(original, reference)
        self.assertIn('ReporterImage: registryImage(constants.ReporterName),', result)
        self.assertIn(reference, result)
        self.assertEqual(pipeline.operator_image(result, reference), result)
        with self.assertRaises(ValueError):
            pipeline.operator_image("unexpected contract", reference)

    def test_operator_update_recovers_when_branch_exists_without_pr(self):
        reference = pipeline.IMAGE + ":1.0.6@sha256:" + "a" * 64
        with tempfile.TemporaryDirectory() as directory:
            settings = Path(directory) / "internal/operands/settings.go"
            settings.parent.mkdir(parents=True)
            settings.write_text(f'ConfigurationsImage: "{reference}",\n', encoding="utf-8")

            def fake_run(*args, cwd=None):
                if args[:3] == ("git", "ls-remote", "--heads"):
                    return "abc123 refs/heads/fix/configurations-1.0.6"
                if args[:3] == ("git", "diff", "--name-only") and len(args) > 3:
                    return "internal/operands/settings.go"
                if args == ("git", "diff", "--name-only"):
                    return ""
                if args[:2] == ("git", "diff"):
                    return ""
                return ""

            with patch.object(pipeline, "run", side_effect=fake_run), patch.object(
                pipeline, "api", side_effect=[[], {}]
            ) as api:
                pipeline.update_operator("1.0.6", "sha256:" + "a" * 64, directory)

        self.assertEqual(api.call_count, 2)
        self.assertEqual(api.call_args_list[1].args[1], "POST")

    def test_release_reservation_survives_a_fresh_checkout(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            remote = root / "remote.git"
            first = root / "first"
            second = root / "second"
            subprocess.check_call(["git", "init", "--bare", str(remote)], stdout=subprocess.DEVNULL)
            subprocess.check_call(["git", "clone", str(remote), str(first)], stdout=subprocess.DEVNULL)
            try:
                os.chdir(first)
                pipeline.run("git", "checkout", "-b", "main")
                pipeline.run("git", "config", "user.name", "CI Test")
                pipeline.run("git", "config", "user.email", "ci@example.invalid")
                Path("pom.xml").write_text(POM, encoding="utf-8")
                application = Path("src/main/resources/application.properties")
                application.parent.mkdir(parents=True)
                application.write_text("quarkus.smallrye-openapi.info-version=1.0.0\n", encoding="utf-8")
                pipeline.run("git", "add", "pom.xml", str(application))
                pipeline.run("git", "commit", "-m", "Initial source")
                pipeline.run("git", "tag", "-a", "v1.0.5", "-m", "Existing release")
                pipeline.run("git", "push", "origin", "HEAD:refs/heads/main", "--tags")
                source = pipeline.run("git", "rev-parse", "HEAD")
                with patch.object(pipeline, "verify_source"), patch.object(pipeline, "output") as first_output:
                    pipeline.reserve(source)
                self.assertEqual(first_output.call_args.kwargs["version"], "1.0.6")
                subprocess.check_call(["git", "clone", "--branch", "main", str(remote), str(second)], stdout=subprocess.DEVNULL)
                os.chdir(second)
                with patch.object(pipeline, "verify_source"), patch.object(pipeline, "output") as second_output:
                    pipeline.reserve(source)
                self.assertEqual(first_output.call_args, second_output.call_args)
                self.assertEqual(pipeline.run("git", "tag", "--list"), "v1.0.5\nv1.0.6")
                self.assertIn("<version>1.0.6</version>", Path("pom.xml").read_text(encoding="utf-8"))
                self.assertIn("info-version=1.0.6", application.read_text(encoding="utf-8"))
            finally:
                os.chdir(previous)

    def test_release_requires_merged_pr_identity(self):
        source = "a" * 40
        pr = {
            "merged_at": "2026-10-19T00:00:00Z",
            "merge_commit_sha": source,
            "base": {"ref": "main", "repo": {"full_name": pipeline.REPOSITORY}},
            "head": {"ref": "fix/test", "repo": {"full_name": pipeline.REPOSITORY}},
        }
        with patch.object(pipeline, "run"), patch.object(pipeline, "api", return_value=[pr]):
            pipeline.verify_source(source)
            pr["head"]["repo"]["full_name"] = "attacker/fork"
            with self.assertRaises(ValueError):
                pipeline.verify_source(source)


if __name__ == "__main__":
    unittest.main()