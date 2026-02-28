#!/bin/bash
# Copy renewed certs so kamailio can read them, then restart (CERTBOT_DOMAIN set by certbot)
D="${CERTBOT_DOMAIN:-__DOMAIN__}"
cp "/etc/letsencrypt/live/$D/fullchain.pem" /etc/kamailio/certs/
cp "/etc/letsencrypt/live/$D/privkey.pem" /etc/kamailio/certs/
chown kamailio:kamailio /etc/kamailio/certs/fullchain.pem /etc/kamailio/certs/privkey.pem
chmod 644 /etc/kamailio/certs/fullchain.pem
chmod 640 /etc/kamailio/certs/privkey.pem
systemctl restart kamailio

systemctl reload apache2
