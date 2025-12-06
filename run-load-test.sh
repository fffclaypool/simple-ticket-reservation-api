#!/bin/bash
# Local JMeter load test script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JMETER_HOME="${JMETER_HOME:-/opt/apache-jmeter-5.6.3}"
RESULTS_DIR="${SCRIPT_DIR}/build/jmeter-results"

echo "=== Building application ==="
./gradlew build -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck

echo "=== Stopping any existing instance ==="
lsof -i :8080 2>/dev/null | grep LISTEN | awk '{print $2}' | xargs -r kill -9 2>/dev/null || true
sleep 2

echo "=== Starting application ==="
nohup java -jar build/libs/ticket-reservation-api-0.0.1-SNAPSHOT.jar > /tmp/app.log 2>&1 &
APP_PID=$!
echo "Application PID: $APP_PID"

echo "=== Waiting for application to be ready ==="
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
        echo "Application is ready"
        break
    fi
    echo "Waiting for application... ($i/30)"
    sleep 2
done

echo "=== Running JMeter load test ==="
mkdir -p "$RESULTS_DIR"
rm -f "$RESULTS_DIR/results.jtl" "$RESULTS_DIR/jmeter.log"
"$JMETER_HOME/bin/jmeter" -n -t jmeter/ticket_booking_load_test.jmx -l "$RESULTS_DIR/results.jtl" -j "$RESULTS_DIR/jmeter.log"

echo "=== Load Test Results ==="
cat "$RESULTS_DIR/results.jtl"
echo ""

echo "=== Checking for overbooking ==="
SUCCESS_COUNT=$(grep "Book Ticket" "$RESULTS_DIR/results.jtl" | grep ",201," | wc -l)
FAIL_COUNT=$(grep "Book Ticket" "$RESULTS_DIR/results.jtl" | grep ",400," | wc -l)
ERROR_COUNT=$(grep "Book Ticket" "$RESULTS_DIR/results.jtl" | grep ",500," | wc -l)
echo "Successful bookings (201): $SUCCESS_COUNT"
echo "No seats available (400): $FAIL_COUNT"
echo "Server errors (500): $ERROR_COUNT"

echo "=== Stopping application ==="
kill $APP_PID 2>/dev/null || true

if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "ERROR: Server errors occurred during load test"
    exit 1
fi
if [ "$SUCCESS_COUNT" -ne 10 ]; then
    echo "ERROR: Expected exactly 10 successful bookings, got $SUCCESS_COUNT"
    exit 1
fi
echo "Load test passed: No overbooking detected"
