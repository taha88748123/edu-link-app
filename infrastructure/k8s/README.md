# NAJA7I-APP — Kubernetes deployment

This directory holds all production-deployment artefacts for the NAJA7I-APP
platform. It is the **only** path to a real K8s cluster — the existing
`docker-compose.yml` at the repo root is dev-only.

```
infrastructure/k8s/
├── helm/naja7i/         umbrella chart: 13 microservices + frontend
├── sealed-secrets/      how to install kubeseal + seal naja7i-secrets
├── cert-manager/        ClusterIssuers for Let's Encrypt staging/prod
└── README.md            this file
```

## Architecture in one diagram

```
        Internet
           │
           ▼
   ┌──────────────────┐
   │ ingress-nginx LB │   (TLS terminated here; cert from cert-manager)
   └────────┬─────────┘
            │
   ┌────────┴─────────┐
   │      Ingress     │
   │ naja7i.ma ──► frontend-web (nginx → React SPA bundle)
   │ api.naja7i.ma ──► api-gateway:8080  (also /ws/* for chat WebSockets)
   └────────┬─────────┘
            │
   ┌────────┴─────────────────────────────────────────┐
   │  api-gateway  ──Eureka──►  13 Spring Boot services │
   │                            (auth, profile, ...)    │
   └────────┬─────────────────────────────────────────┘
            │
   ┌────────┴────────────────────────────────────────┐
   │ Stateful infra (in-cluster OR managed):         │
   │   Postgres · MongoDB · Redis · Kafka · MinIO/S3 │
   └─────────────────────────────────────────────────┘
```

## Deploy workflow

### 1. Provision a cluster

* **dev** — [kind](https://kind.sigs.k8s.io/), [k3d](https://k3d.io/),
  or [minikube](https://minikube.sigs.k8s.io/). 4 CPU / 8 GB RAM minimum.
* **staging / prod** — EKS, GKE, AKS, or DigitalOcean K8s.

### 2. Install cluster-level dependencies

```bash
# Ingress controller
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  -n ingress-nginx --create-namespace

# cert-manager (skip for dev if you don't need TLS)
helm repo add jetstack https://charts.jetstack.io
helm upgrade --install cert-manager jetstack/cert-manager \
  -n cert-manager --create-namespace --version v1.16.x \
  --set crds.enabled=true
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-letsencrypt-prod.yaml      # prod only
kubectl apply -f infrastructure/k8s/cert-manager/cluster-issuer-letsencrypt-staging.yaml   # dev/staging

# sealed-secrets controller (skip for dev)
infrastructure/k8s/sealed-secrets/kubeseal-install.sh
```

### 3. Seal & apply the application Secret (staging/prod)

```bash
# In dev the chart auto-renders a placeholder Secret — skip this section.

# Edit infrastructure/k8s/sealed-secrets/sample-naja7i-secrets.yaml with real values.
# (DO NOT commit the edited copy.)
kubectl create namespace naja7i

kubeseal --cert pub.pem -o yaml \
  < /tmp/naja7i-secrets-plain.yaml \
  > infrastructure/k8s/sealed-secrets/sealed-naja7i-secrets.yaml

kubectl apply -f infrastructure/k8s/sealed-secrets/sealed-naja7i-secrets.yaml
```

### 4. Update Helm dependencies and install the chart

```bash
helm dependency update infrastructure/k8s/helm/naja7i

# Pick the env file matching the cluster you're deploying to:
helm upgrade --install naja7i infrastructure/k8s/helm/naja7i \
  --namespace naja7i \
  --create-namespace \
  -f infrastructure/k8s/helm/naja7i/values-prod.yaml \
  --set global.imageTag=$(git rev-parse --short HEAD)
```

### 5. Verify

```bash
# All pods should reach Running / Ready
kubectl get pods -n naja7i -w

# Cert ready
kubectl describe certificate naja7i-tls -n naja7i

# Hit the API health endpoint
curl -fsS https://api.naja7i.ma/actuator/health
```

## Pre-flight checklist for `values-prod.yaml`

Before merging a PR that bumps the prod chart, verify:

- [ ] `global.imageTag` is set to a real, immutable GA tag (never `latest`)
- [ ] Every Bitnami infra subchart is **disabled** in `values-prod.yaml`
      (postgres / mongodb / redis / kafka / minio) — managed services are used
- [ ] `placeholderSecret.enabled: false`
- [ ] A sealed `naja7i-secrets` is already applied in the target cluster
- [ ] `networkPolicy.enabled: true`
- [ ] HPA enabled on every elastic service (gateway, auth, booking, payment,
      chat, notification, frontend-web)
- [ ] PDB enabled on the SPOFs: api-gateway, auth-service, payment-service
- [ ] `cert-manager.io/cluster-issuer: letsencrypt-prod`
- [ ] cert-manager + ingress-nginx + sealed-secrets controllers are running
- [ ] DNS records for `naja7i.ma` and `api.naja7i.ma` point at the ingress LB
- [ ] Prometheus scrape config (cluster observability stack) targets
      `app.kubernetes.io/part-of=naja7i`

## Per-environment quick reference

| Env     | Infra           | TLS issuer            | HPA | PDB             | NetworkPolicy |
|---------|-----------------|-----------------------|-----|-----------------|---------------|
| dev     | in-cluster      | none                  | OFF | OFF             | OFF           |
| staging | mostly managed  | letsencrypt-staging   | OFF | payment only    | ON            |
| prod    | fully managed   | letsencrypt-prod      | ON  | gateway/auth/payment + eureka | ON |

## Local sanity-check commands

```bash
make helm-deps          # helm dependency update
make helm-lint          # helm lint with each values file
make helm-template-dev  # render values-dev.yaml → stdout
```
