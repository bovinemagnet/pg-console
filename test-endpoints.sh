#!/bin/bash
# Test script for PG Console endpoints
# Usage: ./test-endpoints.sh [port] [host]
# Examples:
#   ./test-endpoints.sh                  # Uses localhost:8080
#   ./test-endpoints.sh 9090             # Uses localhost:9090
#   ./test-endpoints.sh 8080 192.168.1.1 # Uses 192.168.1.1:8080

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
NC='\033[0m' # No Colour

test_endpoint() {
    local path="$1"
    local expected="${2:-200}"
    local full_url="${BASE_URL}${path}"

    # Get both status code and body
    response=$(curl -s -w "\n%{http_code}" --max-time 10 "$full_url")
    status=$(echo "$response" | tail -1)
    body=$(echo "$response" | sed '$d')

    # Check for error indicators in response body
    has_error=false
    error_msg=""

    if echo "$body" | grep -qi "TemplateException\|Exception\|500 - Internal Server Error\|Error id\|Stack:"; then
        has_error=true
        error_msg="Exception in response"
    fi

    if [ "$status" == "$expected" ] && [ "$has_error" == "false" ]; then
        echo -e "${GREEN}PASS${NC} ${path} (${status})"
        ((PASSED++))
    elif [ "$status" == "$expected" ] && [ "$has_error" == "true" ]; then
        echo -e "${RED}FAIL${NC} ${path} (${status} but ${error_msg})"
        ((FAILED++))
        ERRORS+=("${path}: ${error_msg}")
    else
        echo -e "${RED}FAIL${NC} ${path} (expected ${expected}, got ${status})"
        ((FAILED++))
        ERRORS+=("${path}: expected ${expected}, got ${status}")
    fi
}

echo "=============================================="
echo "PG Console Endpoint Test Suite"
echo "Host: ${HOST}"
echo "Port: ${PORT}"
echo "Base URL: ${BASE_URL}"
echo "Instance: ${INSTANCE}"
echo "=============================================="
echo ""

echo -e "${YELLOW}=== Main Dashboard Pages ===${NC}"
test_endpoint "/"
test_endpoint "/slow-queries"
test_endpoint "/activity"
test_endpoint "/locks"
test_endpoint "/wait-events"
test_endpoint "/tables"
test_endpoint "/databases"
test_endpoint "/about"

echo ""
echo -e "${YELLOW}=== Analysis Section ===${NC}"
test_endpoint "/index-advisor"
test_endpoint "/query-regressions"
test_endpoint "/table-maintenance"

echo ""
echo -e "${YELLOW}=== Infrastructure Section ===${NC}"
test_endpoint "/replication"
test_endpoint "/infrastructure"

echo ""
echo -e "${YELLOW}=== Data Control Section ===${NC}"
test_endpoint "/logical-replication"
test_endpoint "/cdc"
test_endpoint "/data-lineage"
test_endpoint "/partitions"

echo ""
echo -e "${YELLOW}=== Enterprise Section ===${NC}"
test_endpoint "/comparison"
test_endpoint "/schema-comparison"
test_endpoint "/bookmarks"
test_endpoint "/audit-log"

echo ""
echo -e "${YELLOW}=== Security Section ===${NC}"
test_endpoint "/security"
test_endpoint "/security/roles"
test_endpoint "/security/connections"
test_endpoint "/security/access"
test_endpoint "/security/compliance"
test_endpoint "/security/recommendations"

echo ""
echo -e "${YELLOW}=== Insights Section ===${NC}"
test_endpoint "/insights"
test_endpoint "/insights/anomalies"
test_endpoint "/insights/forecasts"
test_endpoint "/insights/recommendations"
test_endpoint "/insights/runbooks"

echo ""
echo -e "${YELLOW}=== Diagnostics Section (Phase 21) ===${NC}"
test_endpoint "/diagnostics/pipeline-risk"
test_endpoint "/diagnostics/toast-bloat"
test_endpoint "/diagnostics/index-redundancy"
test_endpoint "/diagnostics/statistical-freshness"
test_endpoint "/diagnostics/write-read-ratio"
test_endpoint "/diagnostics/hot-efficiency"
test_endpoint "/diagnostics/correlation"
test_endpoint "/diagnostics/live-charts"
test_endpoint "/diagnostics/xid-wraparound"

echo ""
echo -e "${YELLOW}=== Diagnostics API (Phase 21) ===${NC}"
test_endpoint "/api/diagnostics/pipeline-risk?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/toast-bloat?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/index-redundancy?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/statistical-freshness?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/write-read-ratio?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/hot-efficiency?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/correlation?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/xid-wraparound?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/live-charts/connections?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/live-charts/transactions?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/live-charts/tuples?instance=${INSTANCE}"
test_endpoint "/api/diagnostics/live-charts/cache?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Sparkline API ===${NC}"
test_endpoint "/api/sparkline?metric=connections&instance=${INSTANCE}"
test_endpoint "/api/sparkline?metric=active_queries&instance=${INSTANCE}"
test_endpoint "/api/sparkline?metric=blocked_queries&instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Notifications API ===${NC}"
test_endpoint "/notifications"
test_endpoint "/notifications/channels"
test_endpoint "/notifications/alerts"

echo ""
echo -e "${YELLOW}=== Schema Comparison Endpoints ===${NC}"
test_endpoint "/schema-comparison/profiles"
test_endpoint "/schema-comparison/history"

echo ""
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
