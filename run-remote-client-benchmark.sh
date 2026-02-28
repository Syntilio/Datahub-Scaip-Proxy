#!/usr/bin/env bash
# Run the SCAIP client performance test (Java does the loop; default 100 runs).
# Usage: ./run-remote-client-benchmark.sh
#
# JVM selection (optional):
#   JAVA_HOME=/path/to/jdk17 ./run-remote-client-benchmark.sh
#   JAVA_HOME=/path/to/jdk23 ./run-remote-client-benchmark.sh
#
# JVM tuning (optional): set SCAIP_JVM_OPTS, e.g.:
#   SCAIP_JVM_OPTS="-XX:+UseZGC -Xmx512m" ./run-remote-client-benchmark.sh

set -e
cd "$(dirname "$0")"

export SCAIP_SERVER_HOST=scaip.syntilio.com
export SCAIP_SERVER_PORT=5062
export SCAIP_CLIENT_HOST=0.0.0.0

echo "Packaging JAR..."
mvn package -q -DskipTests

JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
echo "Using: $($JAVA -version 2>&1 | head -1)"
if [ -n "${SCAIP_JVM_OPTS}" ]; then
  exec $JAVA $SCAIP_JVM_OPTS -cp target/scaip-server-1.0.0.jar com.syntilio.scaip.client.ScaipClientBenchmark
else
  exec $JAVA -cp target/scaip-server-1.0.0.jar com.syntilio.scaip.client.ScaipClientBenchmark
fi
