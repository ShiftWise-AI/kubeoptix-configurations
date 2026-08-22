#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

RELEASE_NAME="${RELEASE_NAME:-kubeoptix-db}"
NAMESPACE="${NAMESPACE:-shiftwise-ai}"
CHART_PATH="${CHART_PATH:-${REPO_ROOT}/helm/postgresql}"
POSTGRESQL_PASSWORD="${POSTGRESQL_PASSWORD:-}"
POSTGRESQL_SECRET_NAME="${POSTGRESQL_SECRET_NAME:-kubeoptix-db}"
RUN_POST_INSTALL_CLEANUP="${RUN_POST_INSTALL_CLEANUP:-true}"

generate_postgresql_password() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex 24
    return
  fi

  od -An -N24 -tx1 /dev/urandom | tr -d ' \n'
}

detect_kube_client() {
  if command -v oc >/dev/null 2>&1; then
    echo "oc"
    return
  fi

  if command -v kubectl >/dev/null 2>&1; then
    echo "kubectl"
    return
  fi
}

if [[ -z "${POSTGRESQL_PASSWORD}" ]]; then
  KUBE_CLIENT="$(detect_kube_client || true)"

  if [[ -n "${KUBE_CLIENT}" ]]; then
    POSTGRESQL_PASSWORD="$("${KUBE_CLIENT}" get secret "${POSTGRESQL_SECRET_NAME}" \
      --namespace "${NAMESPACE}" \
      --output 'jsonpath={.data.POSTGRESQL_PASSWORD}' 2>/dev/null | base64 --decode || true)"
  fi

  if [[ -n "${POSTGRESQL_PASSWORD}" ]]; then
    echo "Using existing PostgreSQL password from secret '${POSTGRESQL_SECRET_NAME}'."
  else
    POSTGRESQL_PASSWORD="$(generate_postgresql_password)"
    echo "Generated a random PostgreSQL password for this installation."
    echo "After installation, retrieve it with: oc get secret ${POSTGRESQL_SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.POSTGRESQL_PASSWORD}' | base64 -d"
  fi
fi

if ! command -v helm >/dev/null 2>&1; then
  echo "ERROR: helm command not found in PATH." >&2
  exit 1
fi

helm upgrade --install "${RELEASE_NAME}" "${CHART_PATH}" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  --set-string postgresql.password="${POSTGRESQL_PASSWORD}" \
  "$@"

if [[ "${RUN_POST_INSTALL_CLEANUP}" == "true" ]]; then
  "${REPO_ROOT}/post-install-cleanup.sh" "${NAMESPACE}"
fi
