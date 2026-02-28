#!/bin/bash
set -e

DOMAIN="scaip.syntilio.com"
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

echo "Installing systemd service..."
cp server-config/scaip.service /etc/systemd/system/scaip.service

systemctl daemon-reload
systemctl enable scaip
systemctl start scaip

echo "Creating temporary self-signed TLS certificate..."
mkdir -p /etc/kamailio/certs

openssl req -x509 -nodes -days 30 \
  -newkey rsa:2048 \
  -keyout /etc/kamailio/certs/self.key \
  -out /etc/kamailio/certs/self.crt \
  -subj "/CN=$DOMAIN"

cp server-config/tls-selfsigned.cfg /etc/kamailio/tls.cfg
cp server-config/kamailio.cfg /etc/kamailio/kamailio.cfg

systemctl restart kamailio
systemctl enable kamailio

# Copy all installation logs to home directory
mkdir -p "$HOME_LOGS"
cp "$INSTALL_LOG" "$HOME_LOGS/" 2>/dev/null || true
cp /var/log/cloud-init-output.log "$HOME_LOGS/" 2>/dev/null || true
cp /var/log/cloud-init.log "$HOME_LOGS/" 2>/dev/null || true

echo "Bootstrap complete. Install logs copied to $HOME_LOGS"