# kubeoptix-configurations Helm Chart

This chart installs the `kubeoptix-configurations` Quarkus API on OpenShift.

It creates:

- `StatefulSet` with exactly `1` replica
- `Service` named `configurations-api` on port `8000`
- `PodDisruptionBudget` with `minAvailable: 1`
- OpenShift `ImageStream`
- OpenShift `BuildConfig` cloning `https://github.com/ShiftWise-AI/kubeoptix-configurations.git`

It does not create an OpenShift `Route` and does not create a headless service.

Render manifests:

```bash
helm template kubeoptix-configurations ./helm/configurations-api
```

Render with example values:

```bash
helm template kubeoptix-configurations ./helm/configurations-api \
  --values ./helm/configurations-api/values.exemple.yaml
```

Install:

```bash
helm upgrade --install kubeoptix-configurations ./helm/configurations-api \
  --namespace shiftwise-ai \
  --create-namespace
```

The Git source secret must exist in the same namespace as the `BuildConfig`. The root `install.sh` can copy it from `github-auth` to `shiftwise-ai` before installing the chart.
