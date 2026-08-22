FROM registry.access.redhat.com/ubi10:1785332448 AS builder

WORKDIR /project

USER 0

RUN dnf install -y java-25-openjdk.x86_64 \
    && dnf install -y maven-openjdk25.noarch \
    && dnf update -y \
    && dnf clean all \
    && mkdir -p /project/.m2/repository \
    && chown -R 1001:root /project \
    && chmod -R g+rwX /project

ENV HOME=/project
ENV MAVEN_CONFIG=/project/.m2

USER 1001

COPY --chown=1001:root pom.xml .
COPY --chown=1001:root src ./src

RUN mvn -B -DskipTests -Dmaven.repo.local=/project/.m2/repository package

FROM registry.access.redhat.com/ubi10:1785332448 AS runtime

WORKDIR /work

USER 0
RUN dnf install -y java-25-openjdk-headless.x86_64 \
    && dnf clean all \
    && chown -R 1001:root /work \
    && chmod -R g+rwX /work

COPY --from=builder --chown=1001:root /project/target/quarkus-app/lib/ /work/lib/
COPY --from=builder --chown=1001:root /project/target/quarkus-app/*.jar /work/
COPY --from=builder --chown=1001:root /project/target/quarkus-app/app/ /work/app/
COPY --from=builder --chown=1001:root /project/target/quarkus-app/quarkus/ /work/quarkus/

EXPOSE 8000
USER 1001

ENTRYPOINT ["java", "-jar", "/work/quarkus-run.jar", "-Dquarkus.http.host=0.0.0.0"]