#!/usr/bin/env bash
# Run voic peep test client: calls SIP trunk, sends peep tone, hangs up.
# Override trunk with VOIC_TRUNK_HOST, VOIC_TRUNK_PORT, VOIC_TRUNK_USER.
# Local IP (VOIC_CLIENT_HOST) is auto-detected from route to trunk if unset.
# Usage: ./run-voic-peep.sh

set -e
cd "$(dirname "$0")"

export VOIC_TRUNK_HOST="${VOIC_TRUNK_HOST:-127.0.0.1}"
export VOIC_TRUNK_PORT="${VOIC_TRUNK_PORT:-5060}"
export VOIC_TRUNK_USER="${VOIC_TRUNK_USER:-voic}"
export VOIC_CLIENT_PORT="${VOIC_CLIENT_PORT:-5064}"
export VOIC_RTP_PORT="${VOIC_RTP_PORT:-10000}"
export VOIC_PEEP_HZ="${VOIC_PEEP_HZ:-1000}"
export VOIC_PEEP_MS="${VOIC_PEEP_MS:-200}"
export VOIC_TRANSPORT="${VOIC_TRANSPORT:-udp}"

mvn exec:java@run-voic-peep -q
