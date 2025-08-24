#!/bin/sh

sed "s|\"secret\": \".*\"|\"secret\": \"$KEYCLOAK_CLIENT_SECRET\"|" /opt/keycloak/data/import/realm-export.json > /tmp/realm-export.json
cp /tmp/realm-export.json /opt/keycloak/data/import/realm-export.json

# Start Keycloak as usual
exec /opt/keycloak/bin/kc.sh "$@"/
