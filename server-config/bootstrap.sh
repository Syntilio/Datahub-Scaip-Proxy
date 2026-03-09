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
touch /opt/scaip/secrets/scaip.env
chmod 600 /etc/scaip/secrets.env
chown scaip:scaip /etc/scaip/secrets.env

cp "$JAR" /opt/scaip/runtime/app.jar
# So that User=scaip can run the service and write logs in /home/scaip
chown -R scaip:scaip /opt/scaip/runtime

echo "Installing systemd service (5 backends on 5062–5066, 600MB heap each)..."
cp server-config/scaip@.service /etc/systemd/system/scaip@.service
systemctl daemon-reload
# Replace single-instance scaip if it was previously installed
systemctl stop scaip 2>/dev/null || true
systemctl disable scaip 2>/dev/null || true
# Stop/disable any old backend instances beyond 5066 (e.g. when reducing from 10 to 5)
for p in 5067 5068 5069 5070 5071; do systemctl stop scaip@$p 2>/dev/null || true; systemctl disable scaip@$p 2>/dev/null || true; done
systemctl enable scaip@5062 scaip@5063 scaip@5064 scaip@5065 scaip@5066
systemctl start scaip@5062 scaip@5063 scaip@5064 scaip@5065 scaip@5066

echo "Setting up TLS (Let's Encrypt or self-signed fallback)..."
mkdir -p /etc/kamailio/certs

echo "Deploying landing page to /var/www/html..."
mkdir -p /var/www/html
cp -r server-config/www/* /var/www/html/ 2>/dev/null || true

# TLS: Let's Encrypt disabled; use self-signed for Kamailio. Apache runs HTTP only.
# To re-enable Let's Encrypt + Apache HTTPS, uncomment the certbot block below and remove the self-signed block.
systemctl stop apache2 2>/dev/null || true
# if certbot certonly --standalone \
#   --agree-tos \
#   --non-interactive \
#   -m "$LETSENCRYPT_EMAIL" \
#   -d "$DOMAIN"; then
#   echo "Let's Encrypt certificate obtained for $DOMAIN"
#   mkdir -p /etc/kamailio/certs
#   cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" /etc/kamailio/certs/
#   cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" /etc/kamailio/certs/
#   chown kamailio:kamailio /etc/kamailio/certs/fullchain.pem /etc/kamailio/certs/privkey.pem
#   chmod 644 /etc/kamailio/certs/fullchain.pem
#   chmod 640 /etc/kamailio/certs/privkey.pem
#   cp server-config/tls-letsencrypt.cfg /etc/kamailio/tls.cfg
#   mkdir -p /etc/letsencrypt/renewal-hooks/deploy
#   sed "s/__DOMAIN__/$DOMAIN/g" server-config/letsencrypt-restart-kamailio.sh > /etc/letsencrypt/renewal-hooks/deploy/restart-kamailio.sh
#   chmod +x /etc/letsencrypt/renewal-hooks/deploy/restart-kamailio.sh
#   a2enmod ssl 2>/dev/null || true
#   printf '%s\n' "ServerName $DOMAIN" > /etc/apache2/conf-available/syntilio.conf
#   a2enconf syntilio 2>/dev/null || true
#   sed "s/__DOMAIN__/$DOMAIN/g" server-config/apache-scaip-ssl.conf > /etc/apache2/sites-available/scaip-ssl.conf
#   a2ensite scaip-ssl.conf 2>/dev/null || true
#   systemctl reload apache2 2>/dev/null || true
# else
echo "Using self-signed certificate (Let's Encrypt disabled for Apache)."
openssl req -x509 -nodes -days 30 \
  -newkey rsa:2048 \
  -keyout /etc/kamailio/certs/self.key \
  -out /etc/kamailio/certs/self.crt \
  -subj "/CN=$DOMAIN"
cp server-config/tls-selfsigned.cfg /etc/kamailio/tls.cfg
# fi
systemctl start apache2 2>/dev/null || true

cp server-config/kamailio.cfg /etc/kamailio/kamailio.cfg
cp server-config/dispatcher.list /etc/kamailio/dispatcher.list

echo "Configuring rsyslog for Kamailio..."
mkdir -p /var/log/kamailio
touch /var/log/kamailio/kamailio-errors.log
touch /var/log/kamailio/kamailio-calls.log
chmod 0644 /var/log/kamailio/kamailio-errors.log /var/log/kamailio/kamailio-calls.log
chown -R syslog:adm /var/log/kamailio

cat > /etc/rsyslog.d/10-kamailio.conf <<'EOF'
local0.err    /var/log/kamailio/kamailio-errors.log
local0.info   -/var/log/kamailio/kamailio-calls.log
& stop
EOF

systemctl restart rsyslog
echo "Configuration done rsyslog for Kamailio..."

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