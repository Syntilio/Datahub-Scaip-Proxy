#!/bin/bash
set -e

DOMAIN="scaip.syntilio.com"
LETSENCRYPT_EMAIL="it@syntilio.com"
BACKEND_PORT=5062
SIP_TLS_PORT=5061
APP_DIR="/opt/scaip"
INSTALL_LOG="/var/log/scaip-bootstrap.log"
HOME_LOGS="/root/scaip-install-logs"

# Capture all output to install log (and still show on stdout when run interactively)
log_capture() {
  mkdir -p "$(dirname "$INSTALL_LOG")"
  exec > >(tee -a "$INSTALL_LOG") 2>&1
}
log_capture

echo "Configuring firewall..."
ufw allow OpenSSH
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 5061/tcp
ufw allow 5062/tcp
ufw --force enable

echo "Building SCAIP project..."
cd $APP_DIR
mvn clean package -DskipTests

# Use the packaged fat jar (shade plugin produces scaip-server-<version>.jar)
JAR="$APP_DIR/target/scaip-server-1.0.0.jar"
if [ ! -f "$JAR" ]; then
  JAR=$(find "$APP_DIR/target" -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -n 1)
fi
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "ERROR: No runnable JAR found in target/" >&2
  exit 1
fi

mkdir -p /opt/scaip/runtime
cp "$JAR" /opt/scaip/runtime/app.jar
# So that User=scaip can run the service and write logs in /home/scaip
chown -R scaip:scaip /opt/scaip/runtime

echo "Installing systemd service..."
cp server-config/scaip.service /etc/systemd/system/scaip.service

systemctl daemon-reload
systemctl enable scaip
systemctl start scaip

echo "Setting up TLS (Let's Encrypt or self-signed fallback)..."
mkdir -p /etc/kamailio/certs

echo "Deploying landing page to /var/www/html..."
mkdir -p /var/www/html
cp -r server-config/www/* /var/www/html/ 2>/dev/null || true

# Try Let's Encrypt (requires DNS for $DOMAIN to point to this server's fixed IP)
systemctl stop apache2 2>/dev/null || true
if certbot certonly --standalone \
  --agree-tos \
  --non-interactive \
  -m "$LETSENCRYPT_EMAIL" \
  -d "$DOMAIN"; then
  echo "Let's Encrypt certificate obtained for $DOMAIN"
  # Copy certs to /etc/kamailio/certs so kamailio user can read them (letsencrypt dir is root-only)
  mkdir -p /etc/kamailio/certs
  cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" /etc/kamailio/certs/
  cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" /etc/kamailio/certs/
  chown kamailio:kamailio /etc/kamailio/certs/fullchain.pem /etc/kamailio/certs/privkey.pem
  chmod 644 /etc/kamailio/certs/fullchain.pem
  chmod 640 /etc/kamailio/certs/privkey.pem
  cat > /etc/kamailio/tls.cfg <<EOF
[server:default]
method = TLSv1.2
verify_certificate = no
require_certificate = no
private_key = /etc/kamailio/certs/privkey.pem
certificate = /etc/kamailio/certs/fullchain.pem
EOF
  mkdir -p /etc/letsencrypt/renewal-hooks/deploy
  cat > /etc/letsencrypt/renewal-hooks/deploy/restart-kamailio.sh <<HOOK
#!/bin/bash
# Copy renewed certs so kamailio can read them, then restart (CERTBOT_DOMAIN set by certbot)
D=\${CERTBOT_DOMAIN:-$DOMAIN}
cp "/etc/letsencrypt/live/\$D/fullchain.pem" /etc/kamailio/certs/
cp "/etc/letsencrypt/live/\$D/privkey.pem" /etc/kamailio/certs/
chown kamailio:kamailio /etc/kamailio/certs/fullchain.pem /etc/kamailio/certs/privkey.pem
chmod 644 /etc/kamailio/certs/fullchain.pem
chmod 640 /etc/kamailio/certs/privkey.pem
systemctl restart kamailio
HOOK
  chmod +x /etc/letsencrypt/renewal-hooks/deploy/restart-kamailio.sh
  # Configure Apache for HTTPS and set global ServerName (suppresses AH00558)
  a2enmod ssl 2>/dev/null || true
  echo "ServerName $DOMAIN" > /etc/apache2/conf-available/syntilio.conf
  a2enconf syntilio 2>/dev/null || true
  sed "s/__DOMAIN__/$DOMAIN/g" server-config/apache-scaip-ssl.conf > /etc/apache2/sites-available/scaip-ssl.conf
  a2ensite scaip-ssl.conf 2>/dev/null || true
  systemctl reload apache2 2>/dev/null || true
  # Reload Apache on cert renewal
  cat >> /etc/letsencrypt/renewal-hooks/deploy/restart-kamailio.sh <<'HOOK'

systemctl reload apache2
HOOK
else
  echo "Let's Encrypt failed (check DNS points $DOMAIN to this host). Using self-signed certificate."
  openssl req -x509 -nodes -days 30 \
    -newkey rsa:2048 \
    -keyout /etc/kamailio/certs/self.key \
    -out /etc/kamailio/certs/self.crt \
    -subj "/CN=$DOMAIN"
  cp server-config/tls-selfsigned.cfg /etc/kamailio/tls.cfg
fi
systemctl start apache2 2>/dev/null || true

cp server-config/kamailio.cfg /etc/kamailio/kamailio.cfg

# So systemd tracks Kamailio after it daemonizes (Type=forking + PIDFile)
mkdir -p /etc/systemd/system/kamailio.service.d
cp server-config/kamailio.service.d/override.conf /etc/systemd/system/kamailio.service.d/override.conf
systemctl daemon-reload

systemctl restart kamailio
systemctl enable kamailio

# Copy all installation logs to home directory
mkdir -p "$HOME_LOGS"
cp "$INSTALL_LOG" "$HOME_LOGS/" 2>/dev/null || true
cp /var/log/cloud-init-output.log "$HOME_LOGS/" 2>/dev/null || true
cp /var/log/cloud-init.log "$HOME_LOGS/" 2>/dev/null || true

echo "Bootstrap complete. Install logs copied to $HOME_LOGS"