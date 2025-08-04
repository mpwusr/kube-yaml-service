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
## Make sure you have access to the Rancher K3S cluster
```
michaelwilliams@Michaels-MBP ~ % rdctl shell sudo cat /etc/rancher/k3s/k3s.yaml > ~/k3s.yaml

### Build the image with nerdctl
michaelwilliams@Michaels-MBP ~ % export KUBECONFIG=~/k3s.yaml                               
kubectl get nodes

NAME                   STATUS   ROLES                  AGE     VERSION
lima-rancher-desktop   Ready    control-plane,master   4m44s   v1.32.5+k3s1
michaelwilliams@Michaels-MBP ~ % echo 'export KUBECONFIG=~/k3s.yaml' >> ~/.zshrc
michaelwilliams@Michaels-MBP ~ % source ~/.zshrc
michaelwilliams@Michaels-MBP ~ % kubectl get nodes
NAME                   STATUS   ROLES                  AGE     VERSION
lima-rancher-desktop   Ready    control-plane,master   6m18s   v1.32.5+k3s1
```
## Extract certificate from Kubeconfig (linux command line)
```
kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d > openshift-ca.crt
```
## Extract certificate from Kubeconfig (windows cmd prompt)
```
kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' > encoded.txt
certutil -decode encoded.txt openshift-ca.crt 
```
## Extract certificate from Kubeconfig (windows power shell)
```
$base64 = oc config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}'
[System.Text.Encoding]::ASCII.GetString([System.Convert]::FromBase64String($base64)) | Out-File -Encoding ascii openshift-ca.crt
```
# Build the image with nerdctl

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

