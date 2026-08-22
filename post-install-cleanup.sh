#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="${1:-${NAMESPACE:-shiftwise-ai}}"
CLEANUP_LABEL="${CLEANUP_LABEL:-kubeoptix.io/post-install-cleanup=true}"

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

echo "Post-install cleanup completed."
