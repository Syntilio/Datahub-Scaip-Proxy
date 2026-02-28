#!/bin/bash
set -e

APP_DIR="/opt/scaip"
INSTALL_LOG="/var/log/scaip-client-bootstrap.log"
HOME_LOGS="/root/scaip-install-logs"
CONF_DIR="/etc/scaip-client"
SERVER_CONF="$CONF_DIR/server.conf"
CLIENT_ENV="$CONF_DIR/client.env"

# Capture all output to install log (and still show on stdout when run interactively)
log_capture() {
  mkdir -p "$(dirname "$INSTALL_LOG")"
  exec > >(tee -a "$INSTALL_LOG") 2>&1
}
log_capture

echo "=== SCAIP remote client bootstrap ==="

# Load server address (set by cloud-init or edit /etc/scaip-client/server.conf)
mkdir -p "$CONF_DIR"
if [ -f "$SERVER_CONF" ]; then
  set -a
  # shellcheck source=/dev/null
  source "$SERVER_CONF"
  set +a
fi
SCAIP_SERVER_HOST="${SCAIP_SERVER_HOST:-scaip.syntilio.com}"
SCAIP_SERVER_PORT="${SCAIP_SERVER_PORT:-5062}"

# Discover this machine's external IP so both systems can see each other
echo "Discovering external IP..."
MY_EXTERNAL_IP=$(curl -s --max-time 10 https://ifconfig.me/ip 2>/dev/null || \
  curl -s --max-time 10 https://api.ipify.org 2>/dev/null || \
  curl -s --max-time 10 https://icanhazip.com 2>/dev/null || echo "unknown")
echo "This client's external IP: $MY_EXTERNAL_IP"

# Resolve server host to IP for display (optional)
SERVER_IP="unknown"
if command -v getent >/dev/null 2>&1; then
  SERVER_IP=$(getent ahostsv4 "$SCAIP_SERVER_HOST" 2>/dev/null | awk '/STREAM/ { print $1; exit }' || echo "unknown")
elif command -v dig >/dev/null 2>&1; then
  SERVER_IP=$(dig +short -4 "$SCAIP_SERVER_HOST" 2>/dev/null | head -n1 || echo "unknown")
fi
echo "SCAIP server address: $SCAIP_SERVER_HOST:$SCAIP_SERVER_PORT (resolved: $SERVER_IP)"

# Write env file for the client service (so both systems see each other: client knows server, bind 0.0.0.0)
cat > "$CLIENT_ENV" << EOF
# SCAIP client env - written by bootstrap
SCAIP_SERVER_HOST=$SCAIP_SERVER_HOST
SCAIP_SERVER_PORT=$SCAIP_SERVER_PORT
SCAIP_CLIENT_HOST=0.0.0.0
SCAIP_CLIENT_PORT=5063
SCAIP_TRANSPORT=udp
EOF
chmod 644 "$CLIENT_ENV"

# Save client external IP for reference (e.g. to allow this IP on the server firewall)
echo "CLIENT_EXTERNAL_IP=$MY_EXTERNAL_IP" >> "$CLIENT_ENV"
echo "SERVER_RESOLVED_IP=$SERVER_IP" >> "$CLIENT_ENV"

echo "If the server firewall restricts by IP, allow this client: sudo ufw allow from $MY_EXTERNAL_IP"

echo "Building SCAIP project..."
cd "$APP_DIR"
mvn clean package -DskipTests -q

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
chown -R scaip:scaip /opt/scaip/runtime

echo "Installing run-once-at-boot unit..."
cp "$APP_DIR/client-config/scaip-client.service" /etc/systemd/system/scaip-client.service
# Unit uses EnvironmentFile; remove the CLIENT_EXTERNAL_IP/SERVER_RESOLVED_IP from env file
# so they are not passed to Java (they are for human reference only)
sed -i '/^CLIENT_EXTERNAL_IP=/d' "$CLIENT_ENV"
sed -i '/^SERVER_RESOLVED_IP=/d' "$CLIENT_ENV"

systemctl daemon-reload
systemctl enable scaip-client
systemctl start scaip-client

# Put the client IP back in a separate info file for reference
echo "CLIENT_EXTERNAL_IP=$MY_EXTERNAL_IP" > "$CONF_DIR/client-ip.txt"
echo "SERVER_RESOLVED_IP=$SERVER_IP" >> "$CONF_DIR/client-ip.txt"

# Copy all installation logs to home directory
mkdir -p "$HOME_LOGS"
cp "$INSTALL_LOG" "$HOME_LOGS/" 2>/dev/null || true
cp /var/log/cloud-init-output.log "$HOME_LOGS/" 2>/dev/null || true
cp /var/log/cloud-init.log "$HOME_LOGS/" 2>/dev/null || true

echo "Bootstrap complete. Client will run once at each boot. Install logs: $HOME_LOGS"
echo "Client external IP (for server firewall): $MY_EXTERNAL_IP"
