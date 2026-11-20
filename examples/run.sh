#!/usr/bin/env bash
#
# Computes sunrise and sunset for a coordinate and date.
#
# Usage:
#   ./run.sh --lat 50.0755 --lon 14.4378 --date 2026-06-21 --tz Europe/Prague
#   ./run.sh --lat -33.8688 --lon 151.2093 --tz Australia/Sydney
#
set -euo pipefail

LAT=""
LON=""
DATE=""
TZ_NAME="UTC"

usage() {
    sed -n '3,9p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --lat) LAT="${2:-}"; shift 2 ;;
        --lon) LON="${2:-}"; shift 2 ;;
        --date) DATE="${2:-}"; shift 2 ;;
        --tz) TZ_NAME="${2:-}"; shift 2 ;;
        -h|--help) usage ;;
        *) echo "Unknown argument: $1" >&2; usage ;;
    esac
done

if [[ -z "$LAT" || -z "$LON" ]]; then
    echo "Both --lat and --lon are required." >&2
    usage
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="$ROOT/target/SunRiseSetCalculator.jar"

if [[ ! -f "$JAR" ]]; then
    echo "Jar not found at $JAR. Build it first: ./mvnw -DskipTests package" >&2
    exit 1
fi

ARGS=(-jar "$JAR" --lat "$LAT" --lon "$LON" --tz "$TZ_NAME")
if [[ -n "$DATE" ]]; then
    ARGS+=(--date "$DATE")
fi

exec java "${ARGS[@]}"
