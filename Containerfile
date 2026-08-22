FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25 AS builder

WORKDIR /project

USER 0
RUN mkdir -p /project/.m2/repository \
    && chown -R 1001:root /project \
    && chmod -R g+rwX /project

ENV HOME=/project
ENV MAVEN_CONFIG=/project/.m2

USER 1001

COPY --chown=1001:root .mvn ./.mvn
COPY --chown=1001:root mvnw pom.xml ./
COPY --chown=1001:root src ./src

RUN ./mvnw -B -DskipTests -Dnative -Dquarkus.native.container-build=false \
    -Dmaven.repo.local=/project/.m2/repository package

FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0

WORKDIR /work/

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

COPY --from=builder --chown=1001:root --chmod=0755 /project/target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]