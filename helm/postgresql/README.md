# kubeoptix-db Helm Chart

This chart installs a single PostgreSQL instance using the Red Hat image `registry.redhat.io/rhel9/postgresql-18:9.8-1787043471`.

## Render manifests

```bash
helm template kubeoptix-db ./helm/postgresql
```

## Install

```bash
helm upgrade --install kubeoptix-db ./helm/postgresql \
  --namespace shiftwise-ai \
  --create-namespace \
  --set postgresql.password='<change-me>'
```

Or use the installation script from the repository root:

```bash
./install.sh
```

When `POSTGRESQL_PASSWORD` is not provided, the script reuses the password from the existing PostgreSQL secret. If the secret does not exist yet, the script generates a random password for the first installation.

To provide the password explicitly:

```bash
POSTGRESQL_PASSWORD='<change-me>' ./install.sh
```

The script accepts additional Helm arguments after the script name:

```bash
POSTGRESQL_PASSWORD='<change-me>' ./install.sh \
  --set persistence.storageClassName='<storage-class-name>'
```

Environment variables supported by the script:

- `RELEASE_NAME`: Helm release name. Default: `kubeoptix-db`
- `NAMESPACE`: target namespace. Default: `shiftwise-ai`
- `CHART_PATH`: chart path. Default: `./helm/postgresql`
- `POSTGRESQL_PASSWORD`: PostgreSQL password. Optional; generated automatically when absent.
- `POSTGRESQL_SECRET_NAME`: secret name used to reuse an existing password. Default: `kubeoptix-db`
- `RUN_POST_INSTALL_CLEANUP`: run cleanup after install. Default: `true`

## Persistent volume

The chart creates a `20Gi` persistent volume claim automatically through the `StatefulSet` `volumeClaimTemplates` field.

By default, `persistence.storageClassName` is unset so OpenShift uses the default `StorageClass` configured in the target cluster. This keeps the chart generic across OCP clusters with dynamic storage provisioning enabled.

To force a specific storage class only when needed:

```bash
helm upgrade --install kubeoptix-db ./helm/postgresql \
  --namespace shiftwise-ai \
  --create-namespace \
  --set postgresql.password='<change-me>' \
  --set persistence.storageClassName='<storage-class-name>'
```

## Use an existing secret

The existing secret must contain these keys by default:

- `POSTGRESQL_USER`
- `POSTGRESQL_PASSWORD`
- `POSTGRESQL_DATABASE`

```bash
helm upgrade --install kubeoptix-db ./helm/postgresql \
  --namespace shiftwise-ai \
  --create-namespace \
  --set postgresql.existingSecret=kubeoptix-db
```

## Post-install cleanup

The post-install cleanup script removes only temporary `Secret` and `ConfigMap` resources labeled with:

```text
kubeoptix.io/post-install-cleanup=true
```

Run it manually when needed:

```bash
./post-install-cleanup.sh shiftwise-ai
```

The PostgreSQL runtime secret is not removed because it is used by the `StatefulSet`. Helm release secrets are also not removed, so future `helm upgrade` and `helm uninstall` operations continue to work.
