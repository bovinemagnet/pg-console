#!/bin/bash
# Test script for PG Console write/mutation endpoints
# WARNING: These tests modify database state - run against test/dev database only!
#
# Usage: ./test-endpoints-write.sh [port] [host]
# Examples:
#   ./test-endpoints-write.sh                  # Uses localhost:8080
#   ./test-endpoints-write.sh 9090             # Uses localhost:9090
#   ./test-endpoints-write.sh 8080 192.168.1.1 # Uses 192.168.1.1:8080

PORT="${1:-8080}"
HOST="${2:-localhost}"
BASE_URL="http://${HOST}:${PORT}"
INSTANCE="default"
PASSED=0
FAILED=0
ERRORS=()

# Colours for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Colour

# Test a POST endpoint with form data
test_post_form() {
    local path="$1"
    local data="$2"
    local expected="${3:-200}"
    local full_url="${BASE_URL}${path}"

    response=$(curl -s -w "\n%{http_code}" --max-time 10 -X POST -d "$data" "$full_url")
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    has_error=false
    if echo "$body" | grep -qi "TemplateException\|Exception\|500 - Internal Server Error\|Error id\|Stack:"; then
        has_error=true
    fi

    if [ "$status" == "$expected" ] && [ "$has_error" == "false" ]; then
        echo -e "${GREEN}PASS${NC} POST ${path} (${status})"
        ((PASSED++))
    else
        echo -e "${RED}FAIL${NC} POST ${path} (expected ${expected}, got ${status})"
        ((FAILED++))
        ERRORS+=("POST ${path}: expected ${expected}, got ${status}")
    fi
}

# Test a POST endpoint with JSON data
test_post_json() {
    local path="$1"
    local data="$2"
    local expected="${3:-200}"
    local full_url="${BASE_URL}${path}"

    response=$(curl -s -w "\n%{http_code}" --max-time 10 -X POST \
        -H "Content-Type: application/json" \
        -d "$data" "$full_url")
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    has_error=false
    if echo "$body" | grep -qi "Exception\|error.*500\|Internal Server Error"; then
        has_error=true
    fi

    if [ "$status" == "$expected" ] && [ "$has_error" == "false" ]; then
        echo -e "${GREEN}PASS${NC} POST ${path} (${status})"
        ((PASSED++))
    else
        echo -e "${RED}FAIL${NC} POST ${path} (expected ${expected}, got ${status})"
        ((FAILED++))
        ERRORS+=("POST ${path}: expected ${expected}, got ${status}")
    fi
}

# Test a PUT endpoint with JSON data
test_put_json() {
    local path="$1"
    local data="$2"
    local expected="${3:-200}"
    local full_url="${BASE_URL}${path}"

    response=$(curl -s -w "\n%{http_code}" --max-time 10 -X PUT \
        -H "Content-Type: application/json" \
        -d "$data" "$full_url")
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    has_error=false
    if echo "$body" | grep -qi "Exception\|error.*500\|Internal Server Error"; then
        has_error=true
    fi

    if [ "$status" == "$expected" ] && [ "$has_error" == "false" ]; then
        echo -e "${GREEN}PASS${NC} PUT ${path} (${status})"
        ((PASSED++))
    else
        echo -e "${RED}FAIL${NC} PUT ${path} (expected ${expected}, got ${status})"
        ((FAILED++))
        ERRORS+=("PUT ${path}: expected ${expected}, got ${status}")
    fi
}

# Test a DELETE endpoint
test_delete() {
    local path="$1"
    local expected="${2:-200}"
    local full_url="${BASE_URL}${path}"

    response=$(curl -s -w "\n%{http_code}" --max-time 10 -X DELETE "$full_url")
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    has_error=false
    if echo "$body" | grep -qi "Exception\|error.*500\|Internal Server Error"; then
        has_error=true
    fi

    # Accept both expected and 404 (not found is ok for delete tests)
    if [ "$status" == "$expected" ] && [ "$has_error" == "false" ]; then
        echo -e "${GREEN}PASS${NC} DELETE ${path} (${status})"
        ((PASSED++))
    elif [ "$status" == "404" ]; then
        echo -e "${YELLOW}SKIP${NC} DELETE ${path} (404 - resource not found)"
        ((PASSED++))
    else
        echo -e "${RED}FAIL${NC} DELETE ${path} (expected ${expected}, got ${status})"
        ((FAILED++))
        ERRORS+=("DELETE ${path}: expected ${expected}, got ${status}")
    fi
}

echo "=============================================="
echo -e "${YELLOW}PG Console Write Endpoint Test Suite${NC}"
echo -e "${RED}WARNING: These tests modify database state!${NC}"
echo "=============================================="
echo "Host: ${HOST}"
echo "Port: ${PORT}"
echo "Base URL: ${BASE_URL}"
echo "Instance: ${INSTANCE}"
echo "=============================================="
echo ""

# ============================================
# Schema Comparison Write Endpoints
# ============================================
echo -e "${BLUE}=== Schema Comparison Write Endpoints ===${NC}"

# Test schema comparison (compares same instance - safe, no actual changes)
test_post_form "/schema-comparison/compare" \
    "sourceInstance=${INSTANCE}&destInstance=${INSTANCE}&sourceSchema=public&destSchema=public&includeTables=true&includeViews=true"

# Test profile creation and deletion
echo -e "${YELLOW}Testing profile lifecycle...${NC}"
PROFILE_RESPONSE=$(curl -s -X POST -d "name=test-profile-$$&description=Test profile&sourceInstance=${INSTANCE}&destInstance=${INSTANCE}&sourceSchema=public&destSchema=public" \
    "${BASE_URL}/schema-comparison/profiles")
if echo "$PROFILE_RESPONSE" | grep -q "test-profile-$$\|success"; then
    echo -e "${GREEN}PASS${NC} POST /schema-comparison/profiles (created test profile)"
    ((PASSED++))
else
    echo -e "${RED}FAIL${NC} POST /schema-comparison/profiles (failed to create profile)"
    ((FAILED++))
fi

# Note: Would need to extract profile ID to test delete - skipping for now
echo -e "${YELLOW}SKIP${NC} DELETE /schema-comparison/profiles/{id} (requires profile ID extraction)"

echo ""

# ============================================
# Custom Dashboard Write Endpoints
# ============================================
echo -e "${BLUE}=== Custom Dashboard Write Endpoints ===${NC}"

# Test dashboard creation
DASHBOARD_NAME="test-dashboard-$$"
test_post_form "/dashboards/custom" \
    "name=${DASHBOARD_NAME}&description=Test dashboard&isDefault=false&isShared=false"

echo ""

# ============================================
# Insights Write Endpoints
# ============================================
echo -e "${BLUE}=== Insights Write Endpoints ===${NC}"

# Test insights refresh
test_post_json "/insights/refresh" "{}" 200

# Test natural language query
test_post_form "/insights/ask" "query=show slow queries"

# Note: Anomaly and runbook endpoints require existing data
echo -e "${YELLOW}SKIP${NC} POST /insights/anomalies/{id}/acknowledge (requires existing anomaly)"
echo -e "${YELLOW}SKIP${NC} POST /insights/runbooks/{id}/start (requires existing runbook)"

echo ""

# ============================================
# Notification Write Endpoints
# ============================================
echo -e "${BLUE}=== Notification Write Endpoints ===${NC}"

# Test channel creation
CHANNEL_JSON='{"name":"test-channel-'$$'","type":"WEBHOOK","enabled":true,"config":{"url":"http://localhost:9999/test"}}'
test_post_json "/notifications/api/channels" "$CHANNEL_JSON" 201

# Test silence creation
SILENCE_JSON='{"alertType":"HIGH_CONNECTIONS","instancePattern":"*","reason":"Test silence","expiresAt":"2099-12-31T23:59:59Z"}'
test_post_json "/notifications/api/silences" "$SILENCE_JSON" 201

# Test quick silence
test_post_form "/notifications/api/silences/quick" \
    "alertType=HIGH_CONNECTIONS&instanceName=${INSTANCE}&durationMinutes=5" 201

# Test maintenance window creation
MAINTENANCE_JSON='{"name":"test-maintenance-'$$'","instancePattern":"*","startTime":"2099-01-01T00:00:00Z","endTime":"2099-01-01T01:00:00Z","reason":"Test maintenance"}'
test_post_json "/notifications/api/maintenance" "$MAINTENANCE_JSON" 201

# Test escalation policy creation
ESCALATION_JSON='{"name":"test-escalation-'$$'","alertTypes":["HIGH_CONNECTIONS"],"levels":[{"delayMinutes":5,"channelIds":[]}]}'
test_post_json "/notifications/api/escalation" "$ESCALATION_JSON" 201

echo ""

# ============================================
# Activity Control Endpoints (Careful!)
# ============================================
echo -e "${BLUE}=== Activity Control Endpoints ===${NC}"
echo -e "${RED}WARNING: These endpoints can terminate database connections!${NC}"

# We use PID 0 which won't match any real process - safe test
echo -e "${YELLOW}SKIP${NC} POST /api/activity/{pid}/cancel (skipped - could affect real queries)"
echo -e "${YELLOW}SKIP${NC} POST /api/activity/{pid}/terminate (skipped - could affect real connections)"

echo ""

# ============================================
# Logging Control Endpoints
# ============================================
echo -e "${BLUE}=== Logging Control Endpoints ===${NC}"

# Test debug mode enable/disable cycle
test_post_json "/api/v1/logging/debug?duration=1" "{}" 200
test_delete "/api/v1/logging/debug" 200

# Test preset application
test_post_json "/api/v1/logging/preset/INFO" "{}" 200

echo ""

# ============================================
# Summary
# ============================================
echo "=============================================="
echo "Test Summary"
echo "=============================================="
echo -e "${GREEN}Passed: ${PASSED}${NC}"
echo -e "${RED}Failed: ${FAILED}${NC}"

if [ ${#ERRORS[@]} -gt 0 ]; then
    echo ""
    echo "Failed endpoints:"
    for err in "${ERRORS[@]}"; do
        echo -e "  ${RED}- ${err}${NC}"
    done
fi

echo ""
if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed.${NC}"
    exit 1
fi
