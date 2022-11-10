#!/bin/bash

set -e

PATH=/bin:/usr/bin

if test $# -ne 4; then
    echo usage: $(basename "$0") cadir destdir nodename nodeip
    exit 1
fi

cadir="$1"
destdir="$2"
nodename="$3"
nodeip="$4"

serverkey="$destdir/${nodename}_serverkey.pem"
clientkey="$destdir/${nodename}_clientkey.pem"
servercert="$destdir/${nodename}_servercert.pem"
clientcert="$destdir/${nodename}_clientcert.pem"

test -s "$serverkey" && test -s "$servercert" && \
    { echo "Server key already generated"; exit 0; }

mkdir -p "$destdir"

TMP=`mktemp -t gencerts-$nodeip.XXXXXX`
trap "rm $TMP* 2>/dev/null" 0

# generate server key & certificate
certtool --generate-privkey --outfile="$serverkey"
chmod 400 "$serverkey"

echo "organization = zstack"   > $TMP
echo "cn = $nodename"         >> $TMP
echo "ip_address = $nodeip"   >> $TMP
echo "tls_www_server"         >> $TMP
echo "encryption_key"         >> $TMP
echo "signing_key"            >> $TMP
echo "expiration_days = 3650" >> $TMP

certtool --generate-certificate --load-privkey "$serverkey" \
         --load-ca-certificate "$cadir/cacert.pem" \
         --load-ca-privkey "$cadir/cakey.pem" \
         --template "$TMP" \
         --outfile "$servercert"

# generate client key & certificate
certtool --generate-privkey --outfile="$clientkey"
chmod 400 "$clientkey"

echo "organization = zstack"   > $TMP
echo "country = CN"           >> $TMP
echo "state = Shanghai"       >> $TMP
echo "locality = Shanghai"    >> $TMP
echo "tls_www_client"         >> $TMP
echo "encryption_key"         >> $TMP
echo "signing_key"            >> $TMP
echo "expiration_days = 3650" >> $TMP

certtool --generate-certificate --load-privkey "$clientkey" \
         --load-ca-certificate "$cadir/cacert.pem" \
         --load-ca-privkey "$cadir/cakey.pem" \
         --template "$TMP" \
         --outfile "$clientcert"
