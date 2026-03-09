#!/usr/bin/env bash
# NACK flow demo: start server with forward failure simulated, then run client.
# Valid messages (alarm, heartbeat) will get NACK (snu=5, "Forward failed") because
# the server does not get 2xx from the forward endpoint.
# Usage: ./run-nack-flow.sh

set -e
cd "$(dirname "$0")"

echo "=============================================="
echo "NACK flow demo (forward failure simulated)"
echo "=============================================="
echo "Server will be started with SCAIP_FORWARD_SIMULATE_NON200=true"
echo "-> Valid SCAIP messages will receive NACK (snu=5, ste=Forward failed)"
echo "-> Invalid messages will still receive NACK (missing tag / invalid XML)"
echo ""

export SCAIP_HOST="${SCAIP_HOST:-127.0.0.1}"
export SCAIP_PORT="${SCAIP_PORT:-5060}"
export SCAIP_FORWARD_SIMULATE_NON200=true

mvn compile -q

echo "Starting server in background..."
mvn exec:java -q &
SERVER_PID=$!
trap 'kill $SERVER_PID 2>/dev/null || true' EXIT

echo "Waiting for server to bind..."
sleep 5

echo ""
echo "Running client (expect NACK for first two messages, then NACK for invalid ones)..."
echo ""

export SCAIP_TRANSPORT=udp
export SCAIP_SERVER_HOST=127.0.0.1
export SCAIP_SERVER_PORT=5060
export SCAIP_CLIENT_HOST=127.0.0.1
mvn exec:java@run-client -q

echo ""
echo "Done. Server stopped."
