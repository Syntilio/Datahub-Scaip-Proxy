#!/usr/bin/env bash
# Run SCAIP client.
#   ./run-remote-client.sh         — TLS via local stunnel (127.0.0.1:15061 -> remote:5061)
#   ./run-remote-client.sh nossl   — TCP to remote:5060 (no TLS)
# (macOS: brew install stunnel for TLS mode)

set -e
cd "$(dirname "$0")"

export SCAIP_TRANSPORT=tcp

if [ "${1:-}" = "nossl" ]; then
  export SCAIP_SERVER_HOST="${SCAIP_SERVER_HOST:-scaip.syntilio.com}"
  export SCAIP_SERVER_PORT="${SCAIP_SERVER_PORT:-5060}"
  # Use 0.0.0.0 so outgoing TCP connection isn't bound to 127.0.0.1 (server would then send reply to its own loopback)
  export SCAIP_CLIENT_HOST=0.0.0.0
else
  export SCAIP_SERVER_HOST=127.0.0.1
  export SCAIP_SERVER_PORT=15061
  export SCAIP_CLIENT_HOST=0.0.0.0
  # Start stunnel if not already listening on 15061
  if ! nc -z 127.0.0.1 15061 2>/dev/null; then
    if command -v stunnel >/dev/null 2>&1; then
      stunnel server-config/stunnel-client-scaip.conf
      sleep 1
    else
      echo "stunnel not found. Install it (e.g. brew install stunnel) or run with: $0 nossl" >&2
      exit 1
    fi
  fi
fi

mvn compile -q
export MAVEN_OPTS="${SCAIP_JVM_OPTS:-} ${MAVEN_OPTS:-}"
mvn exec:java@run-client -q