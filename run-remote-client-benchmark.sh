#!/usr/bin/env bash
# Run the SCAIP client performance test (Java does the loop; default 10000 runs).
# Usage: ./run-remote-client-benchmark.sh

set -e
cd "$(dirname "$0")"

export SCAIP_SERVER_HOST=scaip.syntilio.com
export SCAIP_SERVER_PORT=5062
export SCAIP_CLIENT_HOST=0.0.0.0

echo "Packaging JAR..."
mvn package -q -DskipTests

java -cp target/scaip-server-1.0.0.jar com.syntilio.scaip.client.ScaipClientBenchmark
