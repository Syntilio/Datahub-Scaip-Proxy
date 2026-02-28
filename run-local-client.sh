#!/usr/bin/env bash
# Run SCAIP client against local server (127.0.0.1:5062).
# Usage: ./run-local-client.sh

set -e
cd "$(dirname "$0")"

export SCAIP_SERVER_HOST=127.0.0.1
export SCAIP_SERVER_PORT=5062
export SCAIP_CLIENT_HOST=127.0.0.1

mvn exec:java@run-client -q
