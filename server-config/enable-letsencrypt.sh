#!/bin/bash
set -e

DOMAIN="scaip.syntilio.com"
EMAIL="it@syntilio.com"

systemctl stop apache2 || true

certbot certonly --standalone \
  --agree-tos \
  --non-interactive \
  -m $EMAIL \
  -d $DOMAIN

cat > /etc/kamailio/tls.cfg <<EOF
[server:default]
method = TLSv1.2
verify_certificate = no
require_certificate = no
private_key = /etc/letsencrypt/live/$DOMAIN/privkey.pem
certificate = /etc/letsencrypt/live/$DOMAIN/fullchain.pem
EOF

systemctl restart kamailio

mkdir -p /etc/letsencrypt/renewal-hooks/deploy/

cat > /etc/letsencrypt/renewal-hooks/deploy/restart-kamailio.sh <<EOF
#!/bin/bash
systemctl restart kamailio
EOF

chmod +x /etc/letsencrypt/renewal-hooks/deploy/restart-kamailio.sh

echo "Let's Encrypt enabled."