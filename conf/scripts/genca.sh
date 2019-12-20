#!/bin/bash

set -e

PATH=/bin:/usr/bin
OUTDIR=${1:-qemu-certs}
mkdir -p "$OUTDIR"

# DO NOT overwrite the generated certificates.
test -s "$OUTDIR/cakey.pem" && test -s "$OUTDIR/cacert.pem" && \
    { echo "CA already generated"; exit 0; }

certtool --generate-privkey --outfile="$OUTDIR/cakey.pem"
chmod 400 "$OUTDIR/cakey.pem"

TMP=`mktemp -t genca.sh.XXXXXX`
trap "rm $TMP* 2>/dev/null" EXIT

echo "cn = zstack"             > $TMP
echo "ca"                     >> $TMP
echo "cert_signing_key"       >> $TMP
echo "expiration_days = 3650" >> $TMP

certtool --generate-self-signed --load-privkey "$OUTDIR/cakey.pem" \
         --template "$TMP" \
         --outfile "$OUTDIR/cacert.pem"
