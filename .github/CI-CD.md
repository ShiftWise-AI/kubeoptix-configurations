# CI/CD Audit and Operations

## Findings (2026-10-09)

Main push was already a trigger and its latest publication succeeded. Confirmed
gaps: no test/security gates, mutable Actions, publishing independent of GitFlow,
cancellable main runs, and branch protection requiring only `check-flow`, zero
approvals, and no up-to-date branch. These protection gaps affected all 21
GitFlow branches across seven repositories. Repository secrets were empty;
organization secret visibility returned 403.

Java tests additionally needed a PostgreSQL datasource, and `mvnw` was not
executable. The workflow now calls it with Bash and supplies an ephemeral
PostgreSQL 17.11 service. Its `ci-only-ephemeral` password is public test data,
not a production secret. Jackson 2.21.5 and Netty 4.1.136 had blocking CVEs.
Aligned Jackson 2.21.7 and Netty 4.1.137.Final BOMs now precede the unchanged
Red Hat Quarkus BOM. These security overrides use upstream patched artifacts;
confirm support requirements with Red Hat before production rollout, and remove
overrides when the supported platform BOM contains equivalent or newer fixes.

## Pipeline and Publication

PRs to develop/stage/main, pushes to those branches, and merge queues run
GitFlow, JDK 25 Maven verify with tests enabled, workflow syntax validation,
dependency/secret scans, image build, non-root inspection, and final-image
vulnerability/configuration/secret scans. `ci-required` rejects failure,
cancellation, or skipped dependencies. Trivy v0.69.3 blocks HIGH/CRITICAL,
including unfixed vulnerabilities. Actions are SHA-pinned; actionlint v1.7.7 is
checksum-verified. Token permissions are `contents: read`; checkout does not
persist credentials.

Only main pushes and valid `vMAJOR.MINOR.PATCH` tags log in and publish the
scanned local image, without rebuilding, to
`quay.io/parraes/kubeoptix-configurations`. Main keeps `latest` and SHA tags;
releases keep version and SHA tags and must belong to main history. PRs,
stage/develop, and merge queues never access Quay secrets. Main runs are not
actively cancelled; GitHub may coalesce pending concurrent runs.

## GitHub and Quay Setup

Applied and verified for main/stage/develop: `check-flow` and `ci-required` from
GitHub Actions, up-to-date branch, at least one approval, stale-review dismissal,
last-push approval, admin enforcement, and no force pushes/deletion. Existing
checks were preserved. GitHub permits an APPROVE review while checks fail;
protected branches block integration rather than submission of that review.

Publish the workflows on the existing feature branch, obtain a green PR and
independent review to develop, then promote develop -> stage -> main. Existing
PRs without `ci-required` are blocked until the updated workflow runs. Permit
the pinned Actions in organization policy and protect `v*` tag creation with a
maintainer-only ruleset that disallows updates/deletion. `GITHUB_TOKEN`-created
pushes do not trigger another workflow; use an approved GitHub App for release
automation that creates push-triggering tags.

Use a Quay robot with Write only on the destination repository. Set Actions
secrets `QUAY_USERNAME` (full `namespace+robot`) and `QUAY_PASSWORD` (token),
either repository-level or restricted organization secrets including this repo.
Never print tokens, enable shell tracing, or put credentials in source/command
arguments. Rotate through secure prompts and enable Quay scan notifications.

## Validation and Remaining Acceptance

All 14 workflows passed actionlint; aggregate failure cases and 63 GitFlow
scenarios passed; all 21 protections were re-read. Maven verify passed on JDK 25
with PostgreSQL: 7 tests, no failures/errors/skips. Trivy source scans and GitHub
Advisories checks passed after remediation. The final container was not rebuilt
or scanned locally. No changed workflows were committed, pushed, or executed
remotely, and no image was published. Final acceptance requires a blocked failing
PR, a reviewed green promotion, and a main run publishing the exact scanned image.