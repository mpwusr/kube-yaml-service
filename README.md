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

## Build & Run (Local Development)

### Prerequisites

- Java 17+
- Gradle 8.14+ (or use `./gradlew`)
- `helm` CLI installed
- Rancher Desktop with containerd + nerdctl
- Kube config context pointing to K3s (`kubectl config current-context`)
- (Optional) Postman or curl for testing

---

## Build the JAR

```bash
./gradlew clean bootJar
Expected output:

build/libs/kube-yaml-service-0.0.1-SNAPSHOT.jar
```
Build and Run with nerdctl
```
nerdctl build -t kube-yaml-service:latest .
```
Make sure Dockerfile exists and uses build/libs/kube-yaml-service-0.0.1-SNAPSHOT.jar.

Run the container
```
nerdctl run -d -p 8080:8080 --name kube-service kube-yaml-service:latest
```
Test with Postman or curl
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
Returns "Resources applied successfully." or a failure message.

Run Tests
```
./gradlew clean test
```
Kubernetes Deployment (Optional)
To deploy this Spring Boot service into your Rancher Desktop or other Kubernetes cluster:

```
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```
Make sure to:

Mount any needed local YAMLs or Helm charts as volumes

Set service account permissions (if using in-cluster mode)

Swagger UI
If enabled (via springdoc-openapi), Swagger is accessible at:

```
http://localhost:8080/swagger-ui.html
```

## For Rancher Desktop on Mac OSX with containerd

### Build the JAR
On your Mac host (outside the VM):

```bash
./gradlew clean bootJar
```
You should now have:
```bash
build/libs/kube-yaml-service-0.0.1-SNAPSHOT.jar
```

### Build the image with nerdctl
Since Rancher Desktop uses containerd, you need nerdctl to build the image inside the VM’s containerd store:

```bash
nerdctl build -t kube-yaml-service:latest .
```
#### Tip: This image is now stored in the Lima VM’s containerd registry space, so you can use it directly in K3s without pushing to Docker Hub or another registry.

### Create namespace, RBAC, and deployment
Use the consolidated manifests from earlier (00-namespace-rbac.yaml + 10-deployment.yaml), making sure your deployment references:

image: kube-yaml-service:latest
and includes:
imagePullPolicy: IfNotPresent
so K3s uses the local image.

Apply them:

```bash
kubectl apply -f k8s/00-namespace-rbac.yaml
kubectl apply -f k8s/10-deployment.yaml
```

### Verify the pod is running
```bash
kubectl -n kube-yaml-svc get pods
```

You should see something like:
```bash
NAME                                  READY   STATUS    RESTARTS   AGE
kube-yaml-service-6c8d6d5f78-mnxyz    1/1     Running   0          20s
```

### Expose service for host access
K3s in Rancher Desktop runs inside a Lima VM, so NodePort is the easiest way to reach it from macOS:

Edit 10-deployment.yaml service section:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kube-yaml-service
  namespace: kube-yaml-svc
spec:
  selector:
    app: kube-yaml-service
  ports:
    - name: http
      port: 80
      targetPort: 8080
      nodePort: 30080
  type: NodePort
```

Re-apply:

```bash
kubectl apply -f k8s/10-deployment.yaml
```
### Find the VM IP
From macOS:

```bash
rdctl list
```

or directly with Lima:

```bash
limactl list
limactl shell 0 ip addr show eth0
```
You can usually also just use 127.0.0.1:<nodePort> if Rancher Desktop has port forwarding enabled by default.

### Test from macOS
```bash
curl -X POST http://127.0.0.1:30080/kube/apply \
-H "Content-Type: application/json" \
--data-binary @instructions.json
Resources applied successfully.
```
## License
License
MIT
This project is licensed under the MIT License. See LICENSE for details.

