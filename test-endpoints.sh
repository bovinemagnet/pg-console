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
test_endpoint "/?instance=${INSTANCE}"
test_endpoint "/slow-queries?instance=${INSTANCE}"
test_endpoint "/activity?instance=${INSTANCE}"
test_endpoint "/locks?instance=${INSTANCE}"
test_endpoint "/deadlocks?instance=${INSTANCE}"
test_endpoint "/wait-events?instance=${INSTANCE}"
test_endpoint "/tables?instance=${INSTANCE}"
test_endpoint "/databases?instance=${INSTANCE}"
test_endpoint "/about?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Analysis Section ===${NC}"
test_endpoint "/index-advisor?instance=${INSTANCE}"
test_endpoint "/query-regressions?instance=${INSTANCE}"
test_endpoint "/table-maintenance?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Infrastructure Section ===${NC}"
test_endpoint "/replication?instance=${INSTANCE}"
test_endpoint "/infrastructure?instance=${INSTANCE}"
test_endpoint "/config-health?instance=${INSTANCE}"
test_endpoint "/checkpoints?instance=${INSTANCE}"
test_endpoint "/wal-checkpoints?instance=${INSTANCE}"
test_endpoint "/health-check?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Data Control Section ===${NC}"
test_endpoint "/logical-replication?instance=${INSTANCE}"
test_endpoint "/cdc?instance=${INSTANCE}"
test_endpoint "/data-lineage?instance=${INSTANCE}"
test_endpoint "/partitions?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Enterprise Section ===${NC}"
test_endpoint "/comparison?instance=${INSTANCE}"
test_endpoint "/schema-comparison?instance=${INSTANCE}"
test_endpoint "/bookmarks?instance=${INSTANCE}"
test_endpoint "/audit-log?instance=${INSTANCE}"
test_endpoint "/schema-docs?instance=${INSTANCE}"
test_endpoint "/dashboards/custom?instance=${INSTANCE}"
test_endpoint "/dashboards/custom/new?instance=${INSTANCE}"
test_endpoint "/statements-management?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Schema Docs (Phase 23) ===${NC}"
test_endpoint "/schema-docs/api?instance=${INSTANCE}"
test_endpoint "/schema-docs/databases?instance=${INSTANCE}"
test_endpoint "/schema-docs/schemas?instance=${INSTANCE}&database=postgres"
test_endpoint "/schema-docs/generate?instance=${INSTANCE}&database=postgres&schema=public"

echo ""
echo -e "${YELLOW}=== Custom Dashboards (Phase 23) ===${NC}"
test_endpoint "/dashboards/custom/api?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Schema Comparison Extended ===${NC}"
test_endpoint "/schema-comparison/schemas?instance=${INSTANCE}"
test_endpoint "/schema-comparison/schema-summary?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Database Diff (Phase 25) ===${NC}"
test_endpoint "/database-diff?instance=${INSTANCE}"
test_endpoint "/database-diff/databases?instance=${INSTANCE}"
test_endpoint "/database-diff/schemas?instance=${INSTANCE}&database=postgres"
test_endpoint "/database-diff/schema-summary?instance=${INSTANCE}&database=postgres&schema=public"

echo ""
echo -e "${YELLOW}=== Database Diff Export ===${NC}"
# Note: These require valid database params to generate actual content
test_endpoint "/database-diff/export/html?sourceInstance=${INSTANCE}&sourceDatabase=postgres&sourceSchema=public&destInstance=${INSTANCE}&destDatabase=postgres&destSchema=public"
test_endpoint "/database-diff/export/markdown?sourceInstance=${INSTANCE}&sourceDatabase=postgres&sourceSchema=public&destInstance=${INSTANCE}&destDatabase=postgres&destSchema=public"
test_endpoint "/database-diff/export/pdf?sourceInstance=${INSTANCE}&sourceDatabase=postgres&sourceSchema=public&destInstance=${INSTANCE}&destDatabase=postgres&destSchema=public"

echo ""
echo -e "${YELLOW}=== Security Section ===${NC}"
test_endpoint "/security?instance=${INSTANCE}"
test_endpoint "/security/roles?instance=${INSTANCE}"
test_endpoint "/security/connections?instance=${INSTANCE}"
test_endpoint "/security/access?instance=${INSTANCE}"
test_endpoint "/security/compliance?instance=${INSTANCE}"
test_endpoint "/security/recommendations?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Insights Section ===${NC}"
test_endpoint "/insights?instance=${INSTANCE}"
test_endpoint "/insights/anomalies?instance=${INSTANCE}"
test_endpoint "/insights/forecasts?instance=${INSTANCE}"
test_endpoint "/insights/recommendations?instance=${INSTANCE}"
test_endpoint "/insights/runbooks?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Diagnostics Section (Phase 21) ===${NC}"
test_endpoint "/diagnostics/pipeline-risk?instance=${INSTANCE}"
test_endpoint "/diagnostics/toast-bloat?instance=${INSTANCE}"
test_endpoint "/diagnostics/index-redundancy?instance=${INSTANCE}"
test_endpoint "/diagnostics/statistical-freshness?instance=${INSTANCE}"
test_endpoint "/diagnostics/write-read-ratio?instance=${INSTANCE}"
test_endpoint "/diagnostics/hot-efficiency?instance=${INSTANCE}"
test_endpoint "/diagnostics/correlation?instance=${INSTANCE}"
test_endpoint "/diagnostics/live-charts?instance=${INSTANCE}"
test_endpoint "/diagnostics/xid-wraparound?instance=${INSTANCE}"

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
echo -e "${YELLOW}=== Drill-Down Fragments (Phase 21) ===${NC}"
test_endpoint "/fragments/drilldown/connections?instance=${INSTANCE}"
test_endpoint "/fragments/drilldown/active-queries?instance=${INSTANCE}"
test_endpoint "/fragments/drilldown/blocked-queries?instance=${INSTANCE}"
test_endpoint "/fragments/drilldown/cache-hit?instance=${INSTANCE}"
test_endpoint "/fragments/drilldown/longest-query?instance=${INSTANCE}"
test_endpoint "/fragments/drilldown/database-size?instance=${INSTANCE}"
test_endpoint "/fragments/drilldown/table?instance=${INSTANCE}&table=pg_catalog.pg_class"
test_endpoint "/fragments/drilldown/index?instance=${INSTANCE}&index=pg_class_oid_index&table=pg_class"

echo ""
echo -e "${YELLOW}=== Sparkline API ===${NC}"
test_endpoint "/api/sparkline?metric=connections&instance=${INSTANCE}"
test_endpoint "/api/sparkline?metric=active_queries&instance=${INSTANCE}"
test_endpoint "/api/sparkline?metric=blocked_queries&instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Notifications Pages ===${NC}"
test_endpoint "/notifications?instance=${INSTANCE}"
test_endpoint "/notifications/channels?instance=${INSTANCE}"
test_endpoint "/notifications/alerts?instance=${INSTANCE}"
test_endpoint "/notifications/history?instance=${INSTANCE}"
test_endpoint "/notifications/silences?instance=${INSTANCE}"
test_endpoint "/notifications/maintenance?instance=${INSTANCE}"
test_endpoint "/notifications/escalation?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Notifications API ===${NC}"
test_endpoint "/notifications/api/channels"
test_endpoint "/notifications/api/alerts"
test_endpoint "/notifications/api/silences"
test_endpoint "/notifications/api/maintenance"
test_endpoint "/notifications/api/escalation"
test_endpoint "/notifications/api/stats"

echo ""
echo -e "${YELLOW}=== Schema Comparison Endpoints ===${NC}"
test_endpoint "/schema-comparison/profiles?instance=${INSTANCE}"
test_endpoint "/schema-comparison/history?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Schema Comparison Export Endpoints ===${NC}"
# Note: These require valid instance/schema params to generate actual content
test_endpoint "/schema-comparison/export/html?sourceInstance=${INSTANCE}&destInstance=${INSTANCE}&sourceSchema=public&destSchema=public"
test_endpoint "/schema-comparison/export/markdown?sourceInstance=${INSTANCE}&destInstance=${INSTANCE}&sourceSchema=public&destSchema=public"
test_endpoint "/schema-comparison/export/pdf?sourceInstance=${INSTANCE}&destInstance=${INSTANCE}&sourceSchema=public&destSchema=public"

echo ""
echo -e "${YELLOW}=== Insights API ===${NC}"
test_endpoint "/insights/explain/cache_hit_ratio"

echo ""
echo -e "${YELLOW}=== Runbook Export ===${NC}"
# Note: This test assumes execution ID 1 exists from initial data
test_endpoint "/insights/runbooks/execution/1/export/pdf?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== API v1 Core Endpoints ===${NC}"
test_endpoint "/api/v1/overview?instance=${INSTANCE}"
test_endpoint "/api/v1/activity?instance=${INSTANCE}"
test_endpoint "/api/v1/slow-queries?instance=${INSTANCE}"
test_endpoint "/api/v1/locks?instance=${INSTANCE}"
test_endpoint "/api/v1/wait-events?instance=${INSTANCE}"
test_endpoint "/api/v1/tables?instance=${INSTANCE}"
test_endpoint "/api/v1/databases?instance=${INSTANCE}"
test_endpoint "/api/v1/instances"
test_endpoint "/api/v1/health"

echo ""
echo -e "${YELLOW}=== API v1 Analysis Endpoints ===${NC}"
test_endpoint "/api/v1/index-advisor?instance=${INSTANCE}"
test_endpoint "/api/v1/query-regressions?instance=${INSTANCE}"
test_endpoint "/api/v1/table-maintenance?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== API v1 Infrastructure Endpoints ===${NC}"
test_endpoint "/api/v1/replication?instance=${INSTANCE}"
test_endpoint "/api/v1/infrastructure?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== API v1 Security Endpoints ===${NC}"
test_endpoint "/api/v1/security/summary?instance=${INSTANCE}"
test_endpoint "/api/v1/security/roles?instance=${INSTANCE}"
test_endpoint "/api/v1/security/connections?instance=${INSTANCE}"
test_endpoint "/api/v1/security/access?instance=${INSTANCE}"
test_endpoint "/api/v1/security/compliance?instance=${INSTANCE}"
test_endpoint "/api/v1/security/recommendations?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== API v1 Schema Comparison Endpoints ===${NC}"
test_endpoint "/api/v1/schema-comparison/schemas?instance=${INSTANCE}"
test_endpoint "/api/v1/schema-comparison/profiles"
test_endpoint "/api/v1/schema-comparison/history"
test_endpoint "/api/v1/schema-comparison/summary?instance=${INSTANCE}"

echo ""
echo -e "${YELLOW}=== Logging API Endpoints ===${NC}"
test_endpoint "/api/v1/logging/config"
test_endpoint "/api/v1/logging/presets"
# Note: /api/v1/logging/debug is POST/DELETE only (requires auth)

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
