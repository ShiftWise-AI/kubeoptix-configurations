# kubeoptix-configurations

Configuration and deployment assets for `kubeoptix-configurations`.

## Quarkus application

This repository includes a basic Red Hat build of Quarkus application structure using Quarkus `3.33.3.redhat-00001`.

Prerequisites:

- JDK 25
- Maven 3.9+
- Podman or Docker for container builds

Build the application:

```bash
mvn package
```

Build the native executable locally when using a JDK 25 Mandrel/GraalVM distribution with `native-image` available:

```bash
mvn package -Dnative
```

Build the JVM container image for development. The default `Containerfile` uses JDK 25 and packages a traditional Quarkus JVM image.

```bash
podman build -f Containerfile -t kubeoptix-configurations:latest .
```

Build the native container image only in the final stage. `Containerfile.native` uses a JDK 25 Mandrel builder image.

```bash
podman build -f Containerfile.native -t kubeoptix-configurations:native .
```

Run locally:

```bash
podman run --rm -p 8000:8000 kubeoptix-configurations:latest
```

Health endpoint:

```bash
curl http://localhost:8000/q/health
```

Probe endpoints:

```bash
curl http://localhost:8000/q/health/live
curl http://localhost:8000/q/health/ready
curl http://localhost:8000/q/health/started
```

## Install on OpenShift

The default namespace is `shiftwise-ai`.

Install PostgreSQL and the `kubeoptix-configurations` API:

```bash
./install.sh
```

The installer expects the Git source secret `gitlab` to exist in namespace `github-auth`. It copies that secret into `shiftwise-ai` because OpenShift `BuildConfig` source secrets must be in the same namespace as the build.

If those defaults are not available, the installer automatically tries to discover a `kubernetes.io/basic-auth` secret in the target namespace and uses it as the BuildConfig source secret.

Useful overrides:

```bash
NAMESPACE=shiftwise-ai \
GIT_SOURCE_SECRET_NAMESPACE=github-auth \
GIT_SOURCE_SECRET_NAME=gitlab \
./install.sh
```

By default, the installer now waits for the OpenShift build to finish before moving to the next steps. You can control this behavior with:

```bash
WAIT_FOR_APPLICATION_BUILD=true \
APPLICATION_BUILD_TIMEOUT_SECONDS=1800 \
APPLICATION_BUILD_POLL_INTERVAL_SECONDS=5 \
./install.sh
```