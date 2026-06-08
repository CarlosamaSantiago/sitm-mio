#!/bin/bash
source "$( /usr/bin/dirname "$0" )/../_common.sh"
echo "============================================"
echo "  NODO: AuthService  (puerto 10040)"
echo "  Emite JWT HS256"
echo "============================================"
java -cp "$(CP server-admin/auth-service)" edu.icesi.sitmmio.authservice.Main \
  --port 10040 --users admin/users.example.json \
  --secret demo-secret --salt sitm-salt
