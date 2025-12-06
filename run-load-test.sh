#!/bin/bash
# Load test script for both local and CI environments

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/build/jmeter-results"

# CI mode: skip build/start if --ci flag is passed (app already running)
CI_MODE=false
SPRING_PROFILE="test"

while [[ $# -gt 0 ]]; do
    case $1 in
        --ci)
            CI_MODE=true
            shift
            ;;
        --profile)
            SPRING_PROFILE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

cleanup() {
    if [ "$CI_MODE" = false ] && [ -n "$APP_PID" ]; then
        echo "=== Stopping application ==="
        kill $APP_PID 2>/dev/null || true
    fi
}
trap cleanup EXIT

if [ "$CI_MODE" = false ]; then
    echo "=== Building application ==="
    ./gradlew build -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck

    echo "=== Stopping any existing instance ==="
    lsof -i :8080 2>/dev/null | grep LISTEN | awk '{print $2}' | xargs -r kill -9 2>/dev/null || true
    sleep 2

    echo "=== Starting application (profile: $SPRING_PROFILE) ==="
    nohup java -jar build/libs/ticket-reservation-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=$SPRING_PROFILE > /tmp/app.log 2>&1 &
    APP_PID=$!
    echo "Application PID: $APP_PID"

    echo "=== Waiting for application to be ready ==="
    for i in {1..30}; do
        if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
            echo "Application is ready"
            break
        fi
        if [ $i -eq 30 ]; then
            echo "Application failed to start"
            echo "=== Application Log ==="
            cat /tmp/app.log || true
            exit 1
        fi
        echo "Waiting for application... ($i/30)"
        sleep 2
    done
fi

echo "=== Running JMeter load test ==="
mkdir -p "$RESULTS_DIR"
rm -f "$RESULTS_DIR/results.jtl" "$RESULTS_DIR/jmeter.log"
jmeter -n -t jmeter/ticket_booking_load_test.jmx -l "$RESULTS_DIR/results.jtl" -j "$RESULTS_DIR/jmeter.log"

echo "=== Load Test Results ==="
cat "$RESULTS_DIR/results.jtl"
echo ""

echo "=== Checking for overbooking ==="
# Use awk to check the exact field position (responseCode is field 4)
SUCCESS_COUNT=$(awk -F',' '$3 == "Book Ticket" && $4 == "201"' "$RESULTS_DIR/results.jtl" | wc -l)
FAIL_COUNT=$(awk -F',' '$3 == "Book Ticket" && $4 == "400"' "$RESULTS_DIR/results.jtl" | wc -l)
ERROR_COUNT=$(awk -F',' '$3 == "Book Ticket" && $4 == "500"' "$RESULTS_DIR/results.jtl" | wc -l)
echo "Successful bookings (201): $SUCCESS_COUNT"
echo "No seats available (400): $FAIL_COUNT"
echo "Server errors (500): $ERROR_COUNT"

if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "ERROR: Server errors occurred during load test"
    exit 1
fi
if [ "$SUCCESS_COUNT" -ne 10 ]; then
    echo "ERROR: Expected exactly 10 successful bookings, got $SUCCESS_COUNT"
    exit 1
fi
echo "Load test passed: No overbooking detected"
