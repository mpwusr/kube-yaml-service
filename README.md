# kube-yaml-service

A Spring Boot service for applying Kubernetes YAML manifests using OkHttp. Supports POST/PUT/DELETE via a REST API.

## Features

- REST API to apply Kubernetes resources
- In-cluster token authentication
- YAML parsing using SnakeYAML

# Kubernetes YAML/Helm Apply Service

This project provides a RESTful API to apply Kubernetes manifests (YAML, JSON, or Helm charts) to a running Kubernetes cluster. It supports:
- Local development via Rancher Desktop (K3s)
- Cluster-internal authentication via service account token
- `file://` and `https://` URIs
- Helm chart rendering using `helm template`

---

## 🛠 Build & Run (Local Development)

### ✅ Prerequisites

- Java 17+
- Gradle 8.14+ (or use `./gradlew`)
- `helm` CLI installed
- Rancher Desktop with containerd + nerdctl
- Kube config context pointing to K3s (`kubectl config current-context`)
- (Optional) Postman or curl for testing

---

## 📦 Build the JAR

```bash
./gradlew clean bootJar
Expected output:

build/libs/kube-yaml-service-0.0.1-SNAPSHOT.jar
```
🐳 Build and Run with nerdctl
🏗 Build image using nerdctl:
```
nerdctl build -t kube-yaml-service:latest .
```
Make sure Dockerfile exists and uses build/libs/kube-yaml-service-0.0.1-SNAPSHOT.jar.

🚀 Run the container
```
nerdctl run -d -p 8080:8080 --name kube-service kube-yaml-service:latest
```
🔁 Test with Postman or curl
Create instructions.json
```
[
  {
    "uri": "file:///app/deployment.yaml",
    "action": "create"
  }
]
```
Send POST request
```
curl -X POST http://localhost:8080/kube/apply \
  -H "Content-Type: application/json" \
  --data-binary @instructions.json
  ```
🧠 Returns "Resources applied successfully." or a failure message.

🧪 Run Tests
```
./gradlew clean test
```
⚙️ Kubernetes Deployment (Optional)
To deploy this Spring Boot service into your Rancher Desktop or other Kubernetes cluster:

```
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```
Make sure to:

Mount any needed local YAMLs or Helm charts as volumes

Set service account permissions (if using in-cluster mode)

📘 Swagger UI
If enabled (via springdoc-openapi), Swagger is accessible at:

```
http://localhost:8080/swagger-ui.html
```
📝 License
This project is licensed under the MIT License. See LICENSE for details.

Ready to paste into your `README.md`!
## License

MIT