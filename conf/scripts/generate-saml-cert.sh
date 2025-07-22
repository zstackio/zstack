#!/usr/bin/env bash

set -euo pipefail

OUTDIR=./
TMP="$(mktemp -t saml_cert.XXXXXX)"
trap 'rm -f "${TMP}"* 2>/dev/null' EXIT

# Do not overwrite existing certificates
test -s ${OUTDIR}/saml_privkey.pem && \
    { echo "SAML certificates already generated"; exit 0; }

# Generate private key (PKCS#1)
openssl genrsa -out ${OUTDIR}/saml_privkey.pem 2048

# Convert private key to PKCS#8 format
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt \
    -in ${OUTDIR}/saml_privkey.pem \
    -out ${OUTDIR}/saml_privkey_pkcs8.pem

chmod 400 ${OUTDIR}/saml_privkey.pem
chmod 400 ${OUTDIR}/saml_privkey_pkcs8.pem

# Generate self-signed SAML certificate
openssl req -x509 -new -nodes \
    -key ${OUTDIR}/saml_privkey.pem \
    -days 1095 \
    -out ${OUTDIR}/saml_cert.pem \
    -subj "/CN=SAML Certificate"

exit 0