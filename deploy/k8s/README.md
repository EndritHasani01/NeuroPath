# Local Kubernetes Deployment

## Prerequisites
- Docker Desktop (with the Docker Engine running)
- [k3d](https://k3d.io/) v5 or newer on your PATH
- [kubectl](https://kubernetes.io/docs/tasks/tools/) v1.29+

## 1. Create (or recreate) the local cluster
```powershell
# Optional: clean up an existing cluster first
k3d cluster delete neuropath

# Create a new multi-node cluster and expose Traefik on host ports 8080/8443
k3d cluster create neuropath --servers 1 --agents 2 --port "8080:80@loadbalancer" --port "8443:443@loadbalancer"

# Switch kubectl to the freshly created context
kubectl config use-context k3d-neuropath

# Windows/Docker Desktop only: if kubectl cannot reach host.docker.internal, point the API server to localhost
$apiPort = (docker port k3d-neuropath-serverlb 6443/tcp).Split(':')[-1]
kubectl config set-cluster k3d-neuropath --server "https://127.0.0.1:$apiPort"
```

## 2. Build the application images
```powershell
# From the repo root
docker build -t neuropath-backend:local  backend
docker build -t neuropath-frontend:local frontend
docker build -t neuropath-llm:local      llm
```

## 3. Import the images into the k3d cluster
```powershell
k3d image import neuropath-backend:local neuropath-frontend:local neuropath-llm:local -c neuropath
```

## 4. Apply the Kubernetes manifests
```powershell
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s
```

## 5. Verify the rollout
```powershell
kubectl get pods -n neuropath
kubectl get ingress -n neuropath
```
All pods should become `Running` and each ingress should list an IP address.

## 6. Access the services locally
Open the URLs below in your browser (the `localtest.me` domain resolves to `127.0.0.1` automatically):

- Frontend: http://neuropath.localtest.me:8080/
- Backend health probe: http://api.neuropath.localtest.me:8080/actuator/health
- LLM service root: http://llm.neuropath.localtest.me:8080/

If you need to exercise an API endpoint from the host, remember to send the matching `Host` header, e.g.:
```powershell
curl.exe -H "Host: api.neuropath.localtest.me" http://127.0.0.1:8080/api/auth/login
```

## 7. Optional: tear everything down
```powershell
kubectl delete -f deploy/k8s --ignore-not-found
k3d cluster delete neuropath
```
