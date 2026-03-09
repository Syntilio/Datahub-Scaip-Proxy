#!/usr/bin/env bash
# Run SCAIP client (UDP to server, port 5060).

set -e
cd "$(dirname "$0")"

export SCAIP_TRANSPORT=udp
export SCAIP_SERVER_HOST="${SCAIP_SERVER_HOST:-scaip.syntilio.com}"
export SCAIP_SERVER_PORT="${SCAIP_SERVER_PORT:-5060}"
# Use 0.0.0.0 so the server can send responses back to us
export SCAIP_CLIENT_HOST=0.0.0.0

mvn compile -q
export MAVEN_OPTS="${SCAIP_JVM_OPTS:-} ${MAVEN_OPTS:-}"
mvn exec:java@run-client -q