#!/usr/bin/env bash
# Run SCAIP client against local server (127.0.0.1:5060 over UDP).
# Usage: ./run-local-client.sh

set -e
cd "$(dirname "$0")"

export SCAIP_SERVER_HOST=127.0.0.1
export SCAIP_SERVER_PORT=5060
export SCAIP_CLIENT_HOST=127.0.0.1
export SCAIP_CLIENT_PORT=5070
export SCAIP_TRANSPORT=udp

mvn exec:java@run-client
