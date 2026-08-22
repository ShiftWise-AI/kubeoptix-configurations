# Copilot Instructions

This repository contains the `kubeoptix-db` application and its deployment assets.

## Context Discipline

- Keep answers concise and avoid loading broad repository context unless the task requires it.
- Prefer the smallest relevant file set for the current task.
- For Java application work, start in `src/main`, `src/test`, `pom.xml`, and `src/main/resources/application.properties`.
- For container work, use `Containerfile`, `.dockerignore`, `pom.xml`, and the Quarkus build output expectations.
- For database deployment work, use `helm/postgresql`, `install.sh`, and `post-install-cleanup.sh` only when the user explicitly asks about deployment, Helm, OpenShift, PostgreSQL, or installation scripts.
- Do not summarize generated files, dependency caches, build output, IDE metadata, or VCS internals unless they are directly involved in the issue.

## Project Conventions

- The Java application name is `kubeoptix-configurations`.
- The PostgreSQL Helm release and database service name are `kubeoptix-db`.
- The default OpenShift namespace is `shiftwise-ai`.
- The Java application uses Red Hat build of Quarkus `3.33.3.redhat-00001` and requires JDK 25.
- The application container is a native Quarkus image built with `./mvnw -B -DskipTests -Dnative package` in the `Containerfile` build stage.
- Keep Java package names under `com.shiftwise.ai.kubeoptix` unless a feature requires a narrower package.
- Write code comments in English.
- Keep shell scripts POSIX-friendly where practical, but Bash is acceptable for existing root scripts.
- Do not remove PostgreSQL runtime secrets or PVCs automatically because they can contain live application state.

## Validation

- For Java changes, prefer `JAVA_HOME=/usr/lib/jvm/java-25-openjdk PATH=/usr/lib/jvm/java-25-openjdk/bin:$PATH mvn test` when that JDK exists.
- For Helm changes, run `helm lint ./helm/postgresql` and a focused `helm template` check.
- For script changes, run `bash -n install.sh post-install-cleanup.sh`.
