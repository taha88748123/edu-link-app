# Sealed-secrets for NAJA7I

[sealed-secrets](https://github.com/bitnami-labs/sealed-secrets) lets us
commit encrypted Secret manifests to git: the in-cluster controller has the
only private key, kubectl/Helm only ever see ciphertext, and CI can deploy
without access to plaintext.

## Why not raw Secrets?

* Helm rolls back happily if a Secret is part of the release. But shipping
  plaintext through CI or git history is a non-starter.
* Sealed manifests are scoped to a namespace + name by default, so a
  ciphertext copied to another cluster won't decrypt.
* Re-encryption is offline; the controller renews automatically.

## One-time controller install

```bash
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm upgrade --install sealed-secrets sealed-secrets/sealed-secrets \
  --namespace kube-system \
  --set fullnameOverride=sealed-secrets-controller
```

Then install `kubeseal` locally (macOS `brew install kubeseal`,
Linux: grab the binary from the
[releases page](https://github.com/bitnami-labs/sealed-secrets/releases)).

## Fetching the cluster public key

```bash
kubeseal --controller-name=sealed-secrets-controller \
         --controller-namespace=kube-system \
         --fetch-cert > pub.pem
```

`pub.pem` is safe to commit to git or share — it only encrypts; the
controller's private half never leaves the cluster.

## Encrypting `naja7i-secrets`

```bash
# 1) Write your real values into a local file (gitignored!)
cat > /tmp/naja7i-secrets.env <<'EOF'
jwt-secret=$(openssl rand -base64 48)
internal-key=$(openssl rand -hex 32)
postgres-password=<from your managed-DB provisioning>
mongodb-password=...
minio-password=...
anthropic-api-key=sk-ant-...
sendgrid-api-key=SG....
twilio-auth-token=...
livekit-api-secret=...
firebase-service-account-json=$(cat firebase-sa.json)
EOF

# 2) Render a plain Secret manifest (NOT committed)
kubectl create secret generic naja7i-secrets \
  --namespace=naja7i \
  --from-env-file=/tmp/naja7i-secrets.env \
  --dry-run=client -o yaml > /tmp/naja7i-secrets.yaml

# 3) Seal it
kubeseal --cert pub.pem \
         --format yaml \
         < /tmp/naja7i-secrets.yaml \
         > sealed-naja7i-secrets.yaml

# 4) Commit the SEALED manifest
git add sealed-naja7i-secrets.yaml
git commit -m "chore(secrets): seal naja7i-secrets"

# 5) Apply
kubectl apply -f sealed-naja7i-secrets.yaml
```

## Required keys

The Helm chart's service map (see `helm/naja7i/values.yaml`)
references `naja7i-secrets` for the following keys:

| key                              | required by                                   |
|----------------------------------|-----------------------------------------------|
| `jwt-secret` (≥32 chars)         | api-gateway, auth-service                     |
| `internal-key`                   | calendar, pricing, booking, chat, notification |
| `postgres-password`              | every Postgres-backed service                 |
| `mongodb-password`               | chat-service, mongodb subchart                |
| `redis-password`                 | optional — only when redis auth is enabled    |
| `minio-password`                 | user-profile-service, minio subchart          |
| `anthropic-api-key`              | ai-agent-service                              |
| `twilio-auth-token`              | notification-service (SMS / WA)               |
| `sendgrid-api-key`               | notification-service (email)                  |
| `livekit-api-secret`             | video-session-service                         |
| `firebase-service-account-json`  | notification-service (push)                   |

## Files in this directory

| file                          | purpose                                                                 |
|-------------------------------|-------------------------------------------------------------------------|
| `kubeseal-install.sh`         | idempotent installer for the controller + fetches `pub.pem`             |
| `sample-naja7i-secrets.yaml`  | PLAINTEXT template with dummy values — DO NOT commit a real version     |
| `sealed-naja7i-secrets.yaml`  | example sealed manifest with bogus ciphertext — illustrative only       |
