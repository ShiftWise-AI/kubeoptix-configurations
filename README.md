# KubeOptix Configurations API

A Red Hat Quarkus-based REST API for managing system configurations and document metadata in the KubeOptix platform. Provides endpoints for system settings, document management, author tracking, and customer information with health monitoring and OpenAPI documentation.

## Features

- **System Settings Management**: Create, update, and retrieve system-wide configuration including language, API keys, and extraction methods.
- **Document Management**: Full CRUD operations for documents with version control and metadata tracking.
- **Version Control**: Track document versions with markdown content and descriptions.
- **Author & Customer Tracking**: Manage authors and customers associated with documents.
- **Health Checks**: Liveness, readiness, and startup probe endpoints for Kubernetes integration.
- **OpenAPI Documentation**: Automatically generated Swagger UI for API exploration.
- **PostgreSQL Persistence**: Data stored in PostgreSQL with Hibernate ORM (Panache).
- **Container-Ready**: Pre-configured for Docker/Podman with native and JVM image options.
- **Kubernetes-Ready**: Helm charts for OpenShift/Kubernetes deployment with automatic rollout management.

## Requirements

- **Java**: JDK 25 or later
- **Maven**: 3.9 or later
- **Container Runtime**: Podman or Docker (for building images)
- **Kubernetes/OpenShift**: 1.21+ (for production deployment)
- **Helm**: 3.0+ (for Kubernetes deployment)
- **PostgreSQL**: 18+ (database backend; provided via Helm chart)

## Technologies

- **Runtime**: Red Hat build of Quarkus `3.33.3.redhat-00001`
- **Language**: Java 25 (LTS)
- **API Framework**: Quarkus REST (JAX-RS) with MicroProfile OpenAPI
- **ORM**: Hibernate with Panache (simplified persistence)
- **Database**: PostgreSQL 18
- **Container**: RHEL 10 UBI base images
- **Orchestration**: Kubernetes/OpenShift with Helm
- **Health Checks**: MicroProfile Health
- **Testing**: JUnit 5 with REST Assured

## Project Structure

```text
kubeoptix-configurations/
├── src/
│   ├── main/
│   │   ├── java/com/shiftwise/ai/kubeoptix/
│   │   │   ├── documents/          # Document, Version, Author, Customer resources
│   │   │   ├── health/             # Liveness, Readiness, Startup probes
│   │   │   └── settings/           # System Settings resource and enums
│   │   └── resources/
│   │       └── application.properties   # Quarkus configuration
│   └── test/
│       └── java/                   # Integration tests
├── helm/
│   ├── configurations-api/         # Application Helm chart
│   └── postgresql/                 # PostgreSQL database Helm chart
├── Containerfile                   # JVM container image definition
├── Containerfile.native            # Native image container definition
├── compose.yaml                    # Local Docker Compose environment
├── install.sh                      # Automated OpenShift deployment script
├── post-install-cleanup.sh         # Cleanup script for temp resources
├── pom.xml                         # Maven build configuration
└── README.md                       # This file
```

## Configuration

### Quarkus Application Properties

Key configuration parameters in `src/main/resources/application.properties`:

| Property | Description | Default | Environment Variable |
|----------|-------------|---------|----------------------|
| `quarkus.application.name` | Application identifier | `kubeoptix-configurations` | — |
| `quarkus.http.port` | HTTP port | `8000` | — |
| `quarkus.http.host` | HTTP bind address | `0.0.0.0` | — |
| `quarkus.datasource.db-kind` | Database type | `postgresql` | — |
| `quarkus.datasource.username` | DB username | — | `POSTGRESQL_USER` |
| `quarkus.datasource.password` | DB password | — | `POSTGRESQL_PASSWORD` |
| `quarkus.datasource.jdbc.url` | JDBC connection URL | — | `POSTGRESQL_HOST`, `POSTGRESQL_PORT`, `POSTGRESQL_DATABASE` |
| `quarkus.hibernate-orm.database.generation` | Schema generation strategy | `drop-and-create` | — |

### Environment Variables

The application requires these environment variables (typically provided by the PostgreSQL Helm chart Secret):

- `POSTGRESQL_USER`: Database user
- `POSTGRESQL_PASSWORD`: Database password
- `POSTGRESQL_HOST`: Database host (default: `kubeoptix-db`)
- `POSTGRESQL_PORT`: Database port (default: `5432`)
- `POSTGRESQL_DATABASE`: Database name (default: `kubeoptix`)

For local development, these are provided via `.env` file and `compose.yaml`.

### Kubernetes/OpenShift Configuration

The Helm charts automatically configure:

- **Service**: `configurations-api` on port `8000` (ClusterIP)
- **Database Secret**: `kubeoptix-db` with PostgreSQL credentials
- **StatefulSet**: Single replica with automatic image rollout triggers
- **Health Probes**:
  - Liveness: `/q/health/live` (20s delay, 10s period)
  - Readiness: `/q/health/ready` (10s delay, 10s period)
  - Startup: `/q/health/started` (5s delay, 5s period)

## Installation

### Prerequisites

Verify cluster access:

```bash
oc login <cluster-url>
oc whoami
helm version
```

### Install via Helm Script (Recommended)

The included `install.sh` script automates both database and application deployment:

```bash
./install.sh
```

Default behavior:
- Creates namespace `shiftwise-ai` if it doesn't exist
- Installs PostgreSQL database (`kubeoptix-db` release)
- Installs application (`kubeoptix-configurations` release)
- Waits for build completion (1800 seconds timeout by default)
- Applies automatic rollout on image updates

Custom parameters:

```bash
NAMESPACE=custom-namespace \
DATABASE_RELEASE_NAME=custom-db \
APPLICATION_RELEASE_NAME=custom-api \
INSTALL_DATABASE=true \
INSTALL_APPLICATION=true \
WAIT_FOR_APPLICATION_BUILD=true \
APPLICATION_BUILD_TIMEOUT_SECONDS=1800 \
./install.sh
```

### Manual Helm Installation

If you prefer to install components separately:

**Install PostgreSQL:**

```bash
helm install kubeoptix-db ./helm/postgresql \
  --namespace shiftwise-ai \
  --create-namespace
```

**Install Application:**

```bash
helm install kubeoptix-configurations ./helm/configurations-api \
  --namespace shiftwise-ai
```

### Git Source Secret

For OpenShift `BuildConfig` to clone the repository, a Git source secret must exist in namespace `github-auth` with name `gitlab` (configurable). The script automatically copies it to the target namespace.

Verify the secret exists:

```bash
oc get secret gitlab -n github-auth
```

If it doesn't exist, create one:

```bash
oc create secret generic gitlab \
  -n github-auth \
  --from-literal=username=<username> \
  --from-literal=password=<token>
```

## Helm Configuration

### configurations-api Chart

**Chart Name**: `kubeoptix-configurations`
**Version**: `0.1.0`
**App Version**: `0.1.0`

**Key Resources Created**:
- **StatefulSet**: Single-replica application pod with automatic image triggers
- **Service**: ClusterIP service on port `8000` (name: `configurations-api`)
- **ServiceAccount**: For pod identity and RBAC
- **BuildConfig**: (OpenShift only) Triggers builds from Git repository
- **ImageStream**: (OpenShift only) Tracks container images

**Main Parameters** (in `helm/configurations-api/values.yaml`):

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Pod replicas | `1` |
| `statefulSet.enabled` | Use StatefulSet | `true` |
| `image.repository` | Container image repository | `image-registry.openshift-image-registry.svc:5000/shiftwise-ai/kubeoptix-configurations` |
| `image.tag` | Image tag | `latest` |
| `service.port` | Service port | `8000` |
| `containerPort` | Container port | `8000` |
| `database.secretName` | Secret containing DB credentials | `kubeoptix-db` |
| `probes.liveness.*` | Liveness probe settings | Path: `/q/health/live` |
| `probes.readiness.*` | Readiness probe settings | Path: `/q/health/ready` |
| `probes.startup.*` | Startup probe settings | Path: `/q/health/started` |

### postgresql Chart

**Chart Name**: `kubeoptix-db`
**Version**: `0.1.0`
**App Version**: `18` (PostgreSQL version)

**Key Resources Created**:
- **StatefulSet**: PostgreSQL pod with persistent volume
- **Service**: ClusterIP service on port `5432` (name: `kubeoptix-db`)
- **Secret**: Contains `POSTGRESQL_USER`, `POSTGRESQL_PASSWORD`, `POSTGRESQL_DATABASE`
- **PersistentVolumeClaim**: 20Gi storage (configurable)
- **ServiceAccount**: For pod identity

**Main Parameters** (in `helm/postgresql/values.yaml`):

| Parameter | Description | Default |
|-----------|-------------|---------|
| `image.repository` | PostgreSQL image | `rhel9/postgresql-18` |
| `image.tag` | Image tag | `9.8-1787043471` |
| `postgresql.database` | Database name | `kubeoptix` |
| `postgresql.username` | Database user | `kubeoptix` |
| `postgresql.password` | Database password | (32-char hex) |
| `persistence.enabled` | Use persistent volume | `true` |
| `persistence.size` | Volume size | `20Gi` |
| `persistence.storageClassName` | StorageClass | `null` (use default) |
| `resources.requests.cpu` | CPU request | `100m` |
| `resources.requests.memory` | Memory request | `256Mi` |
| `resources.limits.memory` | Memory limit | `512Mi` |

## Running Locally

### Prerequisites

- JDK 25 installed and in PATH
- Maven 3.9+ installed
- Podman or Docker installed
- `.env` file in repository root (should exist with defaults)

### Start PostgreSQL

```bash
podman compose up -d postgresql
```

Verify PostgreSQL is running:

```bash
podman compose ps
podman logs postgresql
```

### Run in Development Mode

```bash
mvn quarkus:dev
```

Quarkus dev mode:
- Auto-reloads on code changes
- Loads environment variables from `.env`
- Runs on `http://localhost:8000`
- Swagger UI available at `http://localhost:8000/q/swagger-ui`

### Verify Application

```bash
# Health check
curl http://localhost:8000/q/health

# Readiness probe
curl http://localhost:8000/q/health/ready

# List system settings (initially empty)
curl http://localhost:8000/system-settings
```

### Stop Local Environment

```bash
podman compose down
```

To also remove PostgreSQL data:

```bash
podman compose down -v
```

## Development

### Build

**JVM build** (faster):

```bash
mvn package
```

Output: `target/quarkus-app/quarkus-run.jar`

**Native build** (requires Mandrel/GraalVM with `native-image`):

```bash
mvn package -Dnative
```

Output: `target/*-runner`

### Run Tests

```bash
mvn test
```

Tests include:
- Document/Version CRUD operations
- System Settings management
- Health check probes
- API response validation

Integration tests:

```bash
mvn verify
```

### Code Quality

Ensure all code is in English:
- Comments
- Log messages
- Error messages
- User-facing text

Follow the existing package structure:
```
com.shiftwise.ai.kubeoptix.{documents,health,settings}
```

## Container

### Build JVM Image

```bash
podman build -f Containerfile -t kubeoptix-configurations:latest .
```

The JVM image:
- Uses RHEL 10 UBI as base
- Includes JDK 25 and Maven
- Runs the traditional Quarkus JVM application (~400MB image)
- Faster build time, slightly higher runtime memory

### Build Native Image

```bash
podman build -f Containerfile.native -t kubeoptix-configurations:native .
```

The native image:
- Uses JDK 25 Mandrel builder in first stage
- Produces minimal executable (~200MB image)
- Significantly faster startup (~500ms)
- Lower memory footprint

**Note**: Native builds are slower but produce smaller, faster images suitable for resource-constrained environments.

### Run Container Locally

**JVM:**

```bash
podman run -d \
  --name kubeoptix-api \
  -p 8000:8000 \
  -e POSTGRESQL_HOST=postgresql \
  -e POSTGRESQL_USER=kubeoptix \
  -e POSTGRESQL_PASSWORD=password \
  -e POSTGRESQL_DATABASE=kubeoptix \
  --network compose_default \
  kubeoptix-configurations:latest
```

**Native:**

```bash
podman run -d \
  --name kubeoptix-api \
  -p 8000:8000 \
  -e POSTGRESQL_HOST=postgresql \
  -e POSTGRESQL_USER=kubeoptix \
  -e POSTGRESQL_PASSWORD=password \
  -e POSTGRESQL_DATABASE=kubeoptix \
  --network compose_default \
  kubeoptix-configurations:native
```

## Deployment

### OpenShift/Kubernetes Deployment

The application uses Helm charts for deployment. The `install.sh` script handles the complete deployment flow:

1. **Database Setup**: PostgreSQL StatefulSet with 20Gi persistent volume
2. **Application Build**: OpenShift BuildConfig (if available) builds from Git
3. **Application Deployment**: Quarkus application StatefulSet
4. **Automatic Rollout**: Pod automatically restarts when new image is available

**Deployment Namespace**: `shiftwise-ai` (configurable)

**Container Image Source**:
- For OpenShift: Built via BuildConfig from Git repository
- For Kubernetes: Manually push image to registry and configure in `values.yaml`

**Verify Deployment**:

```bash
oc get pods -n shiftwise-ai
oc logs -n shiftwise-ai -l app=kubeoptix-configurations -f

# Check health
oc exec -n shiftwise-ai <pod-name> -- \
  curl http://localhost:8000/q/health
```

## Troubleshooting

### Application fails to start

**Check database connectivity**:

```bash
oc logs -n shiftwise-ai <pod-name>
# Look for "connection refused" or "authentication failed"
```

**Verify PostgreSQL is running**:

```bash
oc get pods -n shiftwise-ai -l app=kubeoptix-db
oc logs -n shiftwise-ai <postgres-pod-name>
```

**Verify database credentials**:

```bash
oc get secret kubeoptix-db -n shiftwise-ai -o yaml
```

### Build hangs or times out

**Increase timeout**:

```bash
WAIT_FOR_APPLICATION_BUILD=true \
APPLICATION_BUILD_TIMEOUT_SECONDS=3600 \
./install.sh
```

**Check build logs**:

```bash
oc logs -n shiftwise-ai bc/kubeoptix-configurations -f
```

### Image pull fails

Verify image registry access:

```bash
oc describe is kubeoptix-configurations -n shiftwise-ai
```

For external registries, ensure ImagePullSecret is configured in `values.yaml`.

### Persistent volume not binding

```bash
oc describe pvc -n shiftwise-ai
```

Verify StorageClass availability:

```bash
oc get storageclass
```

## License

This project is part of the KubeOptix platform. Refer to project-level LICENSE file for terms.