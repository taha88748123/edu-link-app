#!/usr/bin/env bash
# kubeseal-install.sh — idempotent installer for the sealed-secrets controller.
#
# Usage:
#   ./kubeseal-install.sh                # installs into kube-system, default name
#   NS=sealed-secrets ./kubeseal-install.sh
#
# Prereqs: kubectl + helm + (optionally) kubeseal on PATH.
set -euo pipefail

NS="${NS:-kube-system}"
RELEASE="${RELEASE:-sealed-secrets}"
CONTROLLER_NAME="${CONTROLLER_NAME:-sealed-secrets-controller}"
PUBKEY_OUT="${PUBKEY_OUT:-pub.pem}"

# ---- Sanity checks ---------------------------------------------------------
command -v kubectl >/dev/null 2>&1 || { echo "[ERR] kubectl not found in PATH"; exit 1; }
command -v helm    >/dev/null 2>&1 || { echo "[ERR] helm not found in PATH";    exit 1; }

echo "[1/4] Adding sealed-secrets Helm repo..."
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets >/dev/null 2>&1 || true
helm repo update sealed-secrets >/dev/null

echo "[2/4] Installing/upgrading controller in namespace '${NS}' as release '${RELEASE}'..."
helm upgrade --install "${RELEASE}" sealed-secrets/sealed-secrets \
  --namespace "${NS}" \
  --create-namespace \
  --set fullnameOverride="${CONTROLLER_NAME}" \
  --wait \
  --timeout 5m

echo "[3/4] Waiting for controller pod to become ready..."
kubectl rollout status deployment/"${CONTROLLER_NAME}" -n "${NS}" --timeout=180s

echo "[4/4] Fetching cluster public key to '${PUBKEY_OUT}'..."
if command -v kubeseal >/dev/null 2>&1; then
  kubeseal \
    --controller-name="${CONTROLLER_NAME}" \
    --controller-namespace="${NS}" \
    --fetch-cert > "${PUBKEY_OUT}"
else
  echo "[WARN] kubeseal CLI not found; skipping pubkey fetch."
  echo "       Install kubeseal then run:"
  echo "       kubeseal --controller-name=${CONTROLLER_NAME} --controller-namespace=${NS} --fetch-cert > ${PUBKEY_OUT}"
fi

echo ""
echo "=============================================================="
echo " Sealed-secrets controller installed."
echo "   namespace : ${NS}"
echo "   release   : ${RELEASE}"
echo "   pubkey    : $(pwd)/${PUBKEY_OUT}"
echo ""
echo " Next: seal your Secret manifest with"
echo "   kubeseal --cert ${PUBKEY_OUT} -o yaml < my-secret.yaml > sealed.yaml"
echo "=============================================================="
