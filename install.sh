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
DATABASE_HELM_ARGS="${DATABASE_HELM_ARGS:-}"
APPLICATION_HELM_ARGS="${APPLICATION_HELM_ARGS:-}"
WAIT_FOR_APPLICATION_BUILD="${WAIT_FOR_APPLICATION_BUILD:-true}"
APPLICATION_BUILD_TIMEOUT_SECONDS="${APPLICATION_BUILD_TIMEOUT_SECONDS:-1800}"
APPLICATION_BUILD_POLL_INTERVAL_SECONDS="${APPLICATION_BUILD_POLL_INTERVAL_SECONDS:-5}"
WAIT_FOR_APPLICATION_ROLLOUT="${WAIT_FOR_APPLICATION_ROLLOUT:-true}"
APPLICATION_ROLLOUT_TIMEOUT_SECONDS="${APPLICATION_ROLLOUT_TIMEOUT_SECONDS:-900}"
APPLICATION_ROLLOUT_POLL_INTERVAL_SECONDS="${APPLICATION_ROLLOUT_POLL_INTERVAL_SECONDS:-5}"
REQUIRE_GIT_SOURCE_SECRET="${REQUIRE_GIT_SOURCE_SECRET:-false}"
BUILD_SOURCE_SECRET_ENABLED="true"

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

check_cluster_access() {
  local kube_client="${1}"

  if [[ -z "${kube_client}" ]]; then
    echo "ERROR: Neither 'oc' nor 'kubectl' was found in PATH." >&2
    return 1
  fi

  if [[ "${kube_client}" == "oc" ]]; then
    if ! oc whoami >/dev/null 2>&1; then
      echo "ERROR: OpenShift cluster is not reachable with current 'oc' context." >&2
      echo "Hint: run 'oc login ...' and verify with 'oc whoami'." >&2
      return 1
    fi
  else
    if ! kubectl cluster-info >/dev/null 2>&1; then
      echo "ERROR: Kubernetes cluster is not reachable with current 'kubectl' context." >&2
      echo "Hint: configure kubeconfig and verify with 'kubectl cluster-info'." >&2
      return 1
    fi
  fi

  if ! helm ls --all-namespaces >/dev/null 2>&1; then
    echo "ERROR: Helm cannot reach the Kubernetes API server with current kube context." >&2
    echo "Hint: verify KUBECONFIG/current-context and test with 'helm ls -A'." >&2
    return 1
  fi
}

wait_for_application_build() {
  local kube_client="${1}"
  local build_name="${2:-}"
  local buildconfig_name=""
  local build_phase=""
  local deadline="0"
  local missing_polls=0

  if [[ "${WAIT_FOR_APPLICATION_BUILD}" != "true" ]]; then
    return
  fi

  if [[ "${kube_client}" != "oc" ]]; then
    echo "Skipping build wait because OpenShift CLI (oc) is not available."
    return
  fi

  if [[ -z "${build_name}" ]]; then
    buildconfig_name="$(oc get buildconfig \
      --namespace "${NAMESPACE}" \
      --selector "app.kubernetes.io/instance=${APPLICATION_RELEASE_NAME}" \
      --output 'jsonpath={.items[0].metadata.name}' 2>/dev/null || true)"

    if [[ -z "${buildconfig_name}" ]]; then
      echo "No BuildConfig found for release '${APPLICATION_RELEASE_NAME}' in namespace '${NAMESPACE}'."
      return
    fi

    build_name="$(oc get buildconfig "${buildconfig_name}" \
      --namespace "${NAMESPACE}" \
      --output 'jsonpath={.metadata.name}-{.status.lastVersion}' 2>/dev/null || true)"
  fi

  # Accept plain names and qualified references such as build.build.openshift.io/<name>.
  build_name="${build_name##*/}"

  if [[ -z "${build_name}" ]]; then
    echo "No build found to wait for in namespace '${NAMESPACE}'."
    return
  fi

  echo "Waiting for build '${build_name}' to finish."
  deadline=$((SECONDS + APPLICATION_BUILD_TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    build_phase="$(oc get builds.build.openshift.io "${build_name}" \
      --namespace "${NAMESPACE}" \
      --output 'jsonpath={.status.phase}' 2>/dev/null || true)"

    case "${build_phase}" in
      Complete)
        echo "Build '${build_name}' completed successfully."
        return
        ;;
      Failed|Error|Cancelled)
        echo "ERROR: Build '${build_name}' finished with phase '${build_phase}'." >&2
        echo "Recent build log:" >&2
        oc logs "build/${build_name}" --namespace "${NAMESPACE}" --tail=100 >&2 || true
        return 1
        ;;
      "")
        missing_polls=$((missing_polls + 1))
        if (( missing_polls >= 6 )); then
          echo "ERROR: Build '${build_name}' was not found in namespace '${NAMESPACE}'." >&2
          return 1
        fi
        ;;
      New|Pending|Running)
        missing_polls=0
        ;;
      *)
        missing_polls=0
        echo "Build '${build_name}' is in phase '${build_phase}', waiting..."
        ;;
    esac

    sleep "${APPLICATION_BUILD_POLL_INTERVAL_SECONDS}"
  done

  echo "ERROR: Timed out waiting for build '${build_name}' after ${APPLICATION_BUILD_TIMEOUT_SECONDS}s." >&2
  oc logs "build/${build_name}" --namespace "${NAMESPACE}" --tail=100 >&2 || true
  return 1
}

start_application_build() {
  local kube_client="${1}"
  local buildconfig_name=""
  local build_name=""

  if [[ "${kube_client}" != "oc" ]]; then
    echo "Skipping build trigger because OpenShift CLI (oc) is not available."
    return
  fi

  buildconfig_name="$(oc get buildconfig \
    --namespace "${NAMESPACE}" \
    --selector "app.kubernetes.io/instance=${APPLICATION_RELEASE_NAME}" \
    --output 'jsonpath={.items[0].metadata.name}' 2>/dev/null || true)"

  if [[ -z "${buildconfig_name}" ]]; then
    echo "No BuildConfig found for release '${APPLICATION_RELEASE_NAME}' in namespace '${NAMESPACE}'. Skipping build trigger."
    return
  fi

  echo "Starting a new build from BuildConfig '${buildconfig_name}'."
  build_name="$(oc start-build "${buildconfig_name}" --namespace "${NAMESPACE}" --output name 2>/dev/null || true)"

  if [[ -z "${build_name}" ]]; then
    echo "ERROR: Failed to start a new build from BuildConfig '${buildconfig_name}'." >&2
    return 1
  fi

  build_name="${build_name##*/}"
  echo "Triggered build '${build_name}'."
  wait_for_application_build "${kube_client}" "${build_name}"
}

wait_for_application_rollout() {
  local kube_client="${1}"
  local workload=""
  local deadline="0"
  local desired_replicas=""
  local ready_replicas=""

  if [[ "${WAIT_FOR_APPLICATION_ROLLOUT}" != "true" ]]; then
    return
  fi

  workload="$("${kube_client}" get statefulset,deployment \
    --namespace "${NAMESPACE}" \
    --selector "app.kubernetes.io/instance=${APPLICATION_RELEASE_NAME}" \
    --output 'jsonpath={.items[0].kind}/{.items[0].metadata.name}' 2>/dev/null || true)"
  workload="$(echo "${workload}" | tr '[:upper:]' '[:lower:]')"

  if [[ -z "${workload}" || "${workload}" == "/" ]]; then
    echo "No workload found for release '${APPLICATION_RELEASE_NAME}' in namespace '${NAMESPACE}'. Skipping rollout wait."
    return
  fi

  echo "Waiting for '${workload}' to become available in namespace '${NAMESPACE}'."
  deadline=$((SECONDS + APPLICATION_ROLLOUT_TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    desired_replicas="$("${kube_client}" get "${workload}" \
      --namespace "${NAMESPACE}" \
      --output 'jsonpath={.spec.replicas}' 2>/dev/null || true)"
    ready_replicas="$("${kube_client}" get "${workload}" \
      --namespace "${NAMESPACE}" \
      --output 'jsonpath={.status.readyReplicas}' 2>/dev/null || true)"

    if [[ -n "${desired_replicas}" && "${ready_replicas:-0}" == "${desired_replicas}" ]]; then
      echo "Workload '${workload}' is ready (${ready_replicas}/${desired_replicas})."
      return
    fi

    sleep "${APPLICATION_ROLLOUT_POLL_INTERVAL_SECONDS}"
  done

  echo "ERROR: Timed out waiting for '${workload}' to become ready after ${APPLICATION_ROLLOUT_TIMEOUT_SECONDS}s." >&2
  "${kube_client}" get pods --namespace "${NAMESPACE}" \
    --selector "app.kubernetes.io/instance=${APPLICATION_RELEASE_NAME}" >&2 || true
  return 1
}

copy_git_source_secret() {
  local kube_client="${1}"
  local source_secret_name="${GIT_SOURCE_SECRET_NAME}"
  local source_namespace="${GIT_SOURCE_SECRET_NAMESPACE}"
  local discovered_namespace=""
  local candidate_secret_name=""
  local common_secret_name=""

  if [[ "${COPY_GIT_SOURCE_SECRET}" != "true" ]]; then
    return
  fi

  if "${kube_client}" get secret "${source_secret_name}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
    echo "Using existing Git source secret '${source_secret_name}' in namespace '${NAMESPACE}'."
    return
  fi

  for common_secret_name in gitlab github-auth github git source-secret scm; do
    if "${kube_client}" get secret "${common_secret_name}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
      echo "Configured Git source secret '${source_secret_name}' was not found in namespace '${NAMESPACE}'."
      echo "Using discovered Git source secret '${common_secret_name}' in namespace '${NAMESPACE}'."
      GIT_SOURCE_SECRET_NAME="${common_secret_name}"
      return
    fi
  done

  candidate_secret_name="$("${kube_client}" get secret \
    --namespace "${NAMESPACE}" \
    --output jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.type}{"\n"}{end}' 2>/dev/null \
    | awk '$2=="kubernetes.io/basic-auth" && $1 ~ /(git|github|gitlab|scm)/ { print $1; exit }' || true)"

  if [[ -z "${candidate_secret_name}" ]]; then
    candidate_secret_name="$("${kube_client}" get secret \
      --namespace "${NAMESPACE}" \
      --output jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.type}{"\n"}{end}' 2>/dev/null \
      | awk '$2=="kubernetes.io/basic-auth" { print $1; exit }' || true)"
  fi

  if [[ -n "${candidate_secret_name}" ]]; then
    echo "Configured Git source secret '${source_secret_name}' was not found in namespace '${NAMESPACE}'."
    echo "Using discovered Git source secret '${candidate_secret_name}' in namespace '${NAMESPACE}'."
    GIT_SOURCE_SECRET_NAME="${candidate_secret_name}"
    return
  fi

  if ! "${kube_client}" get namespace "${source_namespace}" >/dev/null 2>&1; then
    if "${kube_client}" get secret "${source_namespace}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
      echo "Namespace '${source_namespace}' not found, but secret '${source_namespace}' exists in '${NAMESPACE}'."
      echo "Assuming the intended Git source secret name is '${source_namespace}'."
      GIT_SOURCE_SECRET_NAME="${source_namespace}"
      return
    fi

    discovered_namespace="$("${kube_client}" get secret "${source_secret_name}" --all-namespaces --output jsonpath='{range .items[*]}{.metadata.namespace}{"\n"}{end}' 2>/dev/null | head -n1 || true)"
    if [[ -n "${discovered_namespace}" ]]; then
      echo "Namespace '${source_namespace}' not found. Using discovered namespace '${discovered_namespace}' for secret '${source_secret_name}'."
      source_namespace="${discovered_namespace}"
    else
      if [[ "${REQUIRE_GIT_SOURCE_SECRET}" == "true" ]]; then
        echo "ERROR: Git source secret '${source_secret_name}' not found in namespace '${NAMESPACE}', and source namespace '${source_namespace}' does not exist." >&2
        echo "Hint: set GIT_SOURCE_SECRET_NAME and GIT_SOURCE_SECRET_NAMESPACE explicitly for your cluster." >&2
        return 1
      fi

      echo "WARNING: Git source secret not found; continuing with BuildConfig sourceSecret disabled." >&2
      echo "Set REQUIRE_GIT_SOURCE_SECRET=true to fail fast instead." >&2
      BUILD_SOURCE_SECRET_ENABLED="false"
      return
    fi
  fi

  if [[ "${source_namespace}" == "${NAMESPACE}" ]]; then
    if "${kube_client}" get secret "${source_secret_name}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
      echo "Using existing Git source secret '${source_secret_name}' in namespace '${NAMESPACE}'."
      GIT_SOURCE_SECRET_NAME="${source_secret_name}"
      return
    fi
  fi

  echo "Copying Git source secret '${source_secret_name}' from namespace '${source_namespace}' to '${NAMESPACE}'."
  "${kube_client}" get secret "${source_secret_name}" --namespace "${source_namespace}" --output yaml \
    | sed \
      -e "s/^  namespace: .*/  namespace: ${NAMESPACE}/" \
      -e '/^  resourceVersion:/d' \
      -e '/^  uid:/d' \
      -e '/^  creationTimestamp:/d' \
      -e '/^  managedFields:/,$d' \
    | "${kube_client}" apply --namespace "${NAMESPACE}" --filename -

  GIT_SOURCE_SECRET_NAME="${source_secret_name}"
}

KUBE_CLIENT="$(detect_kube_client || true)"

declare -a DATABASE_HELM_ARGS_ARRAY=()
declare -a APPLICATION_HELM_ARGS_ARRAY=("$@")

if [[ -n "${DATABASE_HELM_ARGS}" ]]; then
  read -r -a DATABASE_HELM_ARGS_ARRAY <<< "${DATABASE_HELM_ARGS}"
fi

if [[ -n "${APPLICATION_HELM_ARGS}" ]]; then
  read -r -a APPLICATION_HELM_EXTRA_ARGS_ARRAY <<< "${APPLICATION_HELM_ARGS}"
  APPLICATION_HELM_ARGS_ARRAY+=("${APPLICATION_HELM_EXTRA_ARGS_ARRAY[@]}")
fi

if ! command -v helm >/dev/null 2>&1; then
  echo "ERROR: helm command not found in PATH." >&2
  exit 1
fi

check_cluster_access "${KUBE_CLIENT}"

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

if [[ "${INSTALL_APPLICATION}" == "true" && -n "${KUBE_CLIENT}" ]]; then
  "${KUBE_CLIENT}" create namespace "${NAMESPACE}" >/dev/null 2>&1 || true
  copy_git_source_secret "${KUBE_CLIENT}"
fi

if [[ "${INSTALL_DATABASE}" == "true" ]]; then
  helm upgrade --install "${DATABASE_RELEASE_NAME}" "${DATABASE_CHART_PATH}" \
    --namespace "${NAMESPACE}" \
    --create-namespace \
    --set-string postgresql.password="${POSTGRESQL_PASSWORD}" \
    "${DATABASE_HELM_ARGS_ARRAY[@]}"
fi

if [[ "${INSTALL_APPLICATION}" == "true" ]]; then
  APPLICATION_HELM_COMMAND=(
    helm upgrade --install "${APPLICATION_RELEASE_NAME}" "${APPLICATION_CHART_PATH}"
    --namespace "${NAMESPACE}"
    --create-namespace
    --set "buildConfig.sourceSecret.enabled=${BUILD_SOURCE_SECRET_ENABLED}"
  )

  if [[ "${BUILD_SOURCE_SECRET_ENABLED}" == "true" ]]; then
    APPLICATION_HELM_COMMAND+=(--set-string "buildConfig.sourceSecret.name=${GIT_SOURCE_SECRET_NAME}")
  fi

  APPLICATION_HELM_COMMAND+=("${APPLICATION_HELM_ARGS_ARRAY[@]}")

  "${APPLICATION_HELM_COMMAND[@]}"

  if [[ -n "${KUBE_CLIENT}" ]]; then
    start_application_build "${KUBE_CLIENT}"
    wait_for_application_rollout "${KUBE_CLIENT}"
  fi
fi

if [[ "${RUN_POST_INSTALL_CLEANUP}" == "true" ]]; then
  "${REPO_ROOT}/post-install-cleanup.sh" "${NAMESPACE}"
fi
