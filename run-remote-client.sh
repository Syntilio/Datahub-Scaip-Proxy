#!/usr/bin/env bash
# Run SCAIP client against remote server (scaip.syntilio.com:5062).
# Usage: ./run-remote-client.sh

set -e
cd "$(dirname "$0")"

export SCAIP_SERVER_HOST=scaip.syntilio.com
export SCAIP_SERVER_PORT=5062
export SCAIP_CLIENT_HOST=0.0.0.0

# Ensure project is compiled (needed on fresh deploy / after git pull)
mvn compile -q
mvn exec:java@run-client -q
