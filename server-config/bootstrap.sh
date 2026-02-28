#!/bin/bash
set -e

DOMAIN="scaip.syntilio.com"
BACKEND_PORT=5062
SIP_TLS_PORT=5061
APP_DIR="/opt/scaip"

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

JAR=$(find target -name "*.jar" | head -n 1)

mkdir -p /opt/scaip/runtime
cp $JAR /opt/scaip/runtime/app.jar

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

echo "Bootstrap complete."