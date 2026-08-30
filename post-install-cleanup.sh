#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-${NAMESPACE:-shiftwise-ai}}"
RESOURCE_PREFIX="${2:-${RESOURCE_PREFIX:-kubeoptix}}"
CLEANUP_LABEL="${CLEANUP_LABEL:-kubeoptix.io/post-install-cleanup=true}"
# Matches Helm release metadata, build artifacts and service account secrets of every kubeoptix release.
CLEANUP_REGEX="${CLEANUP_REGEX:-^(sh\.helm\.release\.v1\.)?${RESOURCE_PREFIX}-.*$}"
# Secrets/configmaps consumed at runtime must never be removed.
PROTECTED_REGEX="${PROTECTED_REGEX:-^(${POSTGRESQL_SECRET_NAME:-kubeoptix-db})$}"

if command -v oc >/dev/null 2>&1; then
  KUBE_CLIENT="oc"
elif command -v kubectl >/dev/null 2>&1; then
  KUBE_CLIENT="kubectl"
else
  echo "ERROR: oc or kubectl command not found in PATH." >&2
  exit 1
fi

echo "Removing post-install secrets and configmaps from namespace '${NAMESPACE}' with label '${CLEANUP_LABEL}'."

"${KUBE_CLIENT}" delete secret,configmap \
  --namespace "${NAMESPACE}" \
  --selector "${CLEANUP_LABEL}" \
  --ignore-not-found

cleanup_resources() {
  local kind="${1}"
  local names=()

  mapfile -t names < <(
    "${KUBE_CLIENT}" get "${kind}" \
      --namespace "${NAMESPACE}" \
      --output 'jsonpath={range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null \
      | grep -E "${CLEANUP_REGEX}" \
      | grep -vE "${PROTECTED_REGEX}" || true
  )

  if (( ${#names[@]} == 0 )); then
    echo "No ${kind} matching '${CLEANUP_REGEX}' to remove in namespace '${NAMESPACE}'."
    return
  fi

  echo "Removing ${kind}: ${names[*]}"
  "${KUBE_CLIENT}" delete "${kind}" \
    --namespace "${NAMESPACE}" \
    --ignore-not-found \
    "${names[@]}"
}

if [[ "${KUBE_CLIENT}" == "oc" ]]; then
  # Skip builds that are still active so cleanup never aborts an in-progress build
  # (relevant when RUN_POST_INSTALL_CLEANUP runs without waiting for the build first).
  ACTIVE_BUILD_REGEX='^(New|Pending|Running)$'
  cleanup_builds() {
    local names=()

    mapfile -t names < <(
      "${KUBE_CLIENT}" get builds.build.openshift.io \
        --namespace "${NAMESPACE}" \
        --output 'jsonpath={range .items[*]}{.metadata.name}{"\t"}{.status.phase}{"\n"}{end}' 2>/dev/null \
        | awk -v phase_re="${ACTIVE_BUILD_REGEX}" '$2 !~ phase_re { print $1 }' \
        | grep -E "${CLEANUP_REGEX}" \
        | grep -vE "${PROTECTED_REGEX}" || true
    )

    if (( ${#names[@]} == 0 )); then
      echo "No completed builds.build.openshift.io matching '${CLEANUP_REGEX}' to remove in namespace '${NAMESPACE}'."
      return
    fi

    echo "Removing builds.build.openshift.io: ${names[*]}"
    "${KUBE_CLIENT}" delete builds.build.openshift.io \
      --namespace "${NAMESPACE}" \
      --ignore-not-found \
      "${names[@]}"
  }
  cleanup_builds
fi

cleanup_resources secret
cleanup_resources configmap

# The service account controller recreates dockercfg secrets with a new suffix,
# so report any that came back instead of looping forever.
RECREATED_SECRETS="$("${KUBE_CLIENT}" get secret \
  --namespace "${NAMESPACE}" \
  --output 'jsonpath={range .items[*]}{.metadata.name}{"\n"}{end}' 2>/dev/null \
  | grep -E "${CLEANUP_REGEX}" | grep -vE "${PROTECTED_REGEX}" || true)"

if [[ -n "${RECREATED_SECRETS}" ]]; then
  echo "WARNING: these secrets were recreated by OpenShift and cannot stay deleted while their ServiceAccount exists:"
  echo "${RECREATED_SECRETS}"
fi

echo "Post-install cleanup completed."
