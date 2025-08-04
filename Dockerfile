FROM eclipse-temurin:17-jdk

# Install curl & helm
RUN apt-get update && apt-get install -y curl tar gzip ca-certificates && rm -rf /var/lib/apt/lists/* \
 && HELM_VERSION=v3.14.4 \
 && curl -fsSL https://get.helm.sh/helm-${HELM_VERSION}-linux-arm64.tar.gz -o /tmp/helm.tgz \
 && tar -xzf /tmp/helm.tgz -C /tmp \
 && mv /tmp/linux-arm64/helm /usr/local/bin/helm \
 && chmod +x /usr/local/bin/helm \
 && rm -rf /tmp/*

WORKDIR /app
COPY build/libs/kube-yaml-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

