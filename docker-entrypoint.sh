#!/bin/sh
set -eu
umask 077

# Aiven supplies a project CA certificate. Import it without disabling verification.
if [ -n "${MYSQL_CA_CERT:-}" ]; then
    cert_dir=$(mktemp -d)
    printf '%s\n' "$MYSQL_CA_CERT" > "$cert_dir/mysql-ca.pem"
    keytool -importcert -noprompt -alias mysql-cloud-ca \
        -file "$cert_dir/mysql-ca.pem" \
        -keystore "$cert_dir/mysql-truststore.p12" \
        -storetype PKCS12 -storepass changeit >/dev/null 2>&1
    exec java \
        -Djavax.net.ssl.trustStore="$cert_dir/mysql-truststore.p12" \
        -Djavax.net.ssl.trustStorePassword=changeit \
        -jar /app/app.jar
fi

exec java -jar /app/app.jar
