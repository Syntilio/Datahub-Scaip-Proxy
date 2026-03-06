#!/usr/bin/env bash
# Run SCAIP server locally (listens on 127.0.0.1:5060, UDP/TCP).
# Usage: ./run-local-server.sh
# Then run the client with: ./run-local-client.sh

set -e
cd "$(dirname "$0")"

export SCAIP_HOST="${SCAIP_HOST:-127.0.0.1}"
export SCAIP_PORT="${SCAIP_PORT:-5060}"

mvn compile -q
exec mvn exec:java -q
