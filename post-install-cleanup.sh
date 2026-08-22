#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-${NAMESPACE:-shiftwise-ai}}"
RELEASE_NAME="${2:-${RELEASE_NAME:-kubeoptix-db}}"
CLEANUP_LABEL="${CLEANUP_LABEL:-kubeoptix.io/post-install-cleanup=true}"
HELM_RELEASE_PREFIX="sh.helm.release.v1.${RELEASE_NAME}"

if command -v oc >/dev/null 2>&1; then
  KUBE_CLIENT="oc"
elif command -v kubectl >/dev/null 2>&1; then
  KUBE_CLIENT="kubectl"
else
  echo "ERROR: oc or kubectl command not found in PATH." >&2
  exit 1
fi

echo "Removing temporary post-install secrets and configmaps from namespace '${NAMESPACE}' with label '${CLEANUP_LABEL}'."

"${KUBE_CLIENT}" delete secret,configmap \
  --namespace "${NAMESPACE}" \
  --selector "${CLEANUP_LABEL}" \
  --ignore-not-found

mapfile -t HELM_RELEASE_RESOURCES < <(
  "${KUBE_CLIENT}" get secret,configmap \
    --namespace "${NAMESPACE}" \
    --output name 2>/dev/null \
    | grep -E "^(secret|configmap)/${HELM_RELEASE_PREFIX}(\.|$)" || true
)

if (( ${#HELM_RELEASE_RESOURCES[@]} > 0 )); then
  echo "Removing Helm release metadata for '${RELEASE_NAME}' from namespace '${NAMESPACE}'."
  "${KUBE_CLIENT}" delete \
    --namespace "${NAMESPACE}" \
    --ignore-not-found \
    "${HELM_RELEASE_RESOURCES[@]}"
fi

echo "Post-install cleanup completed."
