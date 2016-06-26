#!/bin/sh

OUTDIR=certs
mkdir -p $OUTDIR

# DO NOT overwrite the generated certificates.
test -s $OUTDIR/privkey.pem && \
    ( echo "certificates already generated"; exit 0 )

openssl genrsa -out $OUTDIR/privkey.pem 2048
chmod 400 $OUTDIR/privkey.pem

# Generate the public key
# openssl rsa -in $OUTDIR/privkey.pem -pubout -out $OUTDIR/pubkey.pem

# Generate the CA certificate
#
# The CN "store.zstack.org" will be used by the client to authenticate
# the server name, do NOT modify it.
openssl req -new -x509 -days 3650 \
	-subj '/C=CN/ST=Shanghai/L=Shanghai/CN=store.zstack.org' \
	-key $OUTDIR/privkey.pem -out $OUTDIR/ca.pem

exit 0
