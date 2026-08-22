#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DATABASE_RELEASE_NAME="${DATABASE_RELEASE_NAME:-kubeoptix-db}"
APPLICATION_RELEASE_NAME="${APPLICATION_RELEASE_NAME:-kubeoptix-configurations}"
NAMESPACE="${NAMESPACE:-shiftwise-ai}"
DATABASE_CHART_PATH="${DATABASE_CHART_PATH:-${REPO_ROOT}/helm/postgresql}"
APPLICATION_CHART_PATH="${APPLICATION_CHART_PATH:-${REPO_ROOT}/helm/configurations-api}"
POSTGRESQL_PASSWORD="${POSTGRESQL_PASSWORD:-}"
POSTGRESQL_SECRET_NAME="${POSTGRESQL_SECRET_NAME:-kubeoptix-db}"
INSTALL_DATABASE="${INSTALL_DATABASE:-true}"
INSTALL_APPLICATION="${INSTALL_APPLICATION:-true}"
COPY_GIT_SOURCE_SECRET="${COPY_GIT_SOURCE_SECRET:-true}"
GIT_SOURCE_SECRET_NAME="${GIT_SOURCE_SECRET_NAME:-gitlab}"
GIT_SOURCE_SECRET_NAMESPACE="${GIT_SOURCE_SECRET_NAMESPACE:-github-auth}"
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

copy_git_source_secret() {
  local kube_client="${1}"

  if [[ "${COPY_GIT_SOURCE_SECRET}" != "true" ]]; then
    return
  fi

  if [[ "${GIT_SOURCE_SECRET_NAMESPACE}" == "${NAMESPACE}" ]]; then
    return
  fi

  if "${kube_client}" get secret "${GIT_SOURCE_SECRET_NAME}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
    echo "Using existing Git source secret '${GIT_SOURCE_SECRET_NAME}' in namespace '${NAMESPACE}'."
    return
  fi

  echo "Copying Git source secret '${GIT_SOURCE_SECRET_NAME}' from namespace '${GIT_SOURCE_SECRET_NAMESPACE}' to '${NAMESPACE}'."
  "${kube_client}" get secret "${GIT_SOURCE_SECRET_NAME}" --namespace "${GIT_SOURCE_SECRET_NAMESPACE}" --output yaml \
    | sed \
      -e "s/^  namespace: .*/  namespace: ${NAMESPACE}/" \
      -e '/^  resourceVersion:/d' \
      -e '/^  uid:/d' \
      -e '/^  creationTimestamp:/d' \
      -e '/^  managedFields:/,$d' \
    | "${kube_client}" apply --namespace "${NAMESPACE}" --filename -
}

KUBE_CLIENT="$(detect_kube_client || true)"

if [[ "${INSTALL_DATABASE}" == "true" && -z "${POSTGRESQL_PASSWORD}" ]]; then

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

if [[ "${INSTALL_APPLICATION}" == "true" && -n "${KUBE_CLIENT}" ]]; then
  "${KUBE_CLIENT}" create namespace "${NAMESPACE}" >/dev/null 2>&1 || true
  copy_git_source_secret "${KUBE_CLIENT}"
fi

if [[ "${INSTALL_DATABASE}" == "true" ]]; then
  helm upgrade --install "${DATABASE_RELEASE_NAME}" "${DATABASE_CHART_PATH}" \
    --namespace "${NAMESPACE}" \
    --create-namespace \
    --set-string postgresql.password="${POSTGRESQL_PASSWORD}" \
    "$@"
fi

if [[ "${INSTALL_APPLICATION}" == "true" ]]; then
  helm upgrade --install "${APPLICATION_RELEASE_NAME}" "${APPLICATION_CHART_PATH}" \
    --namespace "${NAMESPACE}" \
    --create-namespace \
    --set-string buildConfig.sourceSecret.name="${GIT_SOURCE_SECRET_NAME}" \
    "$@"
fi

if [[ "${RUN_POST_INSTALL_CLEANUP}" == "true" ]]; then
  if [[ "${INSTALL_DATABASE}" == "true" ]]; then
    "${REPO_ROOT}/post-install-cleanup.sh" "${NAMESPACE}" "${DATABASE_RELEASE_NAME}"
  fi

  if [[ "${INSTALL_APPLICATION}" == "true" ]]; then
    "${REPO_ROOT}/post-install-cleanup.sh" "${NAMESPACE}" "${APPLICATION_RELEASE_NAME}"
  fi
fi
