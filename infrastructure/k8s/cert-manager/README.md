# cert-manager — TLS automation for NAJA7I

cert-manager watches `Certificate` resources (and Ingress annotations) and
provisions TLS via ACME (Let's Encrypt by default).

## Install the controller

```bash
helm repo add jetstack https://charts.jetstack.io
helm repo update jetstack

helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.16.x \
  --set crds.enabled=true
```

Verify:

```bash
kubectl get pods -n cert-manager
# expect: cert-manager, cert-manager-cainjector, cert-manager-webhook all Running
```

## Pick a ClusterIssuer

The two manifests in this directory are the canonical issuers we use:

| file                                       | use when                                        |
|--------------------------------------------|-------------------------------------------------|
| `cluster-issuer-letsencrypt-staging.yaml`  | dev + staging clusters; testing issuance        |
| `cluster-issuer-letsencrypt-prod.yaml`     | production cluster — real browser-trusted certs |

Before applying, **edit the `spec.acme.email` field** in each file to a real
mailbox you control (Let's Encrypt sends expiry warnings there).

```bash
kubectl apply -f cluster-issuer-letsencrypt-staging.yaml      # dev / staging
kubectl apply -f cluster-issuer-letsencrypt-prod.yaml         # prod only
```

## HTTP-01 vs DNS-01

The included issuers use the **HTTP-01** solver via the `nginx` ingress
class. That requires:

* The cluster's ingress controller is publicly reachable on TCP/80.
* The DNS `A`/`AAAA` records for the host already point at the ingress LB.

If you need wildcard certs (`*.naja7i.ma`) or your cluster is private, switch
to a **DNS-01** solver instead. The minimal change is to add a `dns01:` block
under `solvers:` with credentials for your DNS provider (Route53, Cloudflare,
Google Cloud DNS). See <https://cert-manager.io/docs/configuration/acme/dns01/>.

## Issuing a cert

The Helm chart (`infrastructure/k8s/helm/naja7i`) already includes a
`Certificate` resource that requests certs for `{global.domain,
global.apiDomain}` when both `ingress.tls.enabled` and the
`cert-manager.io/cluster-issuer` annotation are set.

For ad-hoc / one-off testing:

```bash
kubectl apply -n naja7i -f - <<'EOF'
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: naja7i-tls
spec:
  secretName: naja7i-tls
  issuerRef:
    name: letsencrypt-staging
    kind: ClusterIssuer
  dnsNames:
    - staging.naja7i.ma
    - api.staging.naja7i.ma
EOF
```

## Smoke test

```bash
kubectl get certificate -n naja7i
kubectl describe certificate naja7i-tls -n naja7i
# Look for "Ready: True" and an Events log showing ACME challenge success.

kubectl get challenge -A     # should be empty once issuance completes
```

If the cert sticks in "Issuing", check:

```bash
kubectl describe order   -n naja7i
kubectl describe challenge -n naja7i
kubectl logs -n cert-manager deploy/cert-manager
```

Common failures:
- DNS isn't pointing at the ingress yet (HTTP-01 → 404 on `/.well-known/acme-challenge`)
- Wrong `ingress.class` selector on the ClusterIssuer
- Hit the 5-failures-per-hour LE rate limit (switch to staging, fix, retry)
