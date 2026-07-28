#!/usr/bin/env bash
# =============================================================================
# VERG — Organisation (org) Registry End-to-End Test
# =============================================================================
# Tests the full pipeline: CREATE -> Postgres -> Elasticsearch -> Redis
#                          -> READ -> SEARCH -> DELETE (soft delete)
#
# NOTE: the generated org catalogue exposes create / read / search / delete /
#       import. There is intentionally NO update endpoint, so update is not
#       tested here.
#
# Prerequisites:
#   1. Docker Compose services running:  docker compose up -d
#   2. Spring Boot app running:          mvn spring-boot:run   (or ./mvnw ...)
#
# Usage:
#   bash tests/test_org_registry.sh
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
APP_BASE="http://localhost:8080"
ES_BASE="http://localhost:9200"
ES_INDEX="org_index"

ORG_CREATE_URL="${APP_BASE}/org/v1/create"
ORG_READ_URL="${APP_BASE}/org/v1/read"
ORG_SEARCH_URL="${APP_BASE}/org/v1/search"
ORG_DELETE_URL="${APP_BASE}/org/v1/delete"

# Docker container names come from docker-compose.yml (oas-*)
PG_CONTAINER="oas-postgres"
REDIS_CONTAINER="oas-redis"
PG_USER="oas_user"
PG_DB="oas_db"

PASS="OK"
FAIL="XX"
TOTAL=0
PASSED=0
FAILED=0

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
print_header() {
    echo ""
    echo "================================================================"
    echo "  $1"
    echo "================================================================"
}

print_result() {
    TOTAL=$((TOTAL + 1))
    if [ "$1" = "pass" ]; then
        PASSED=$((PASSED + 1))
        echo "  [${PASS}] $2"
    else
        FAILED=$((FAILED + 1))
        echo "  [${FAIL}] $2"
    fi
}

# ---------------------------------------------------------------------------
# Pre-flight checks
# ---------------------------------------------------------------------------
print_header "PRE-FLIGHT CHECKS"

PREFLIGHT_OK=true

if curl -s --connect-timeout 3 "${APP_BASE}" > /dev/null 2>&1; then
    echo "  + Spring Boot App is reachable"
else
    echo "  - Spring Boot App is NOT reachable at ${APP_BASE}"
    PREFLIGHT_OK=false
fi

if curl -s --connect-timeout 3 "${ES_BASE}" > /dev/null 2>&1; then
    echo "  + Elasticsearch is reachable"
else
    echo "  - Elasticsearch is NOT reachable at ${ES_BASE}"
    PREFLIGHT_OK=false
fi

if docker exec "${REDIS_CONTAINER}" redis-cli ping 2>/dev/null | grep -q "PONG"; then
    echo "  + Redis is reachable (via docker exec ${REDIS_CONTAINER})"
else
    echo "  - Redis is NOT reachable"
    PREFLIGHT_OK=false
fi

if docker exec "${PG_CONTAINER}" pg_isready -U "${PG_USER}" -d "${PG_DB}" > /dev/null 2>&1; then
    echo "  + PostgreSQL is reachable (via docker exec ${PG_CONTAINER})"
else
    echo "  - PostgreSQL is NOT reachable"
    PREFLIGHT_OK=false
fi

if [ "$PREFLIGHT_OK" = false ]; then
    echo ""
    echo "  Some services are unreachable. Make sure:"
    echo "    1. docker compose up -d"
    echo "    2. mvn spring-boot:run"
    echo ""
    echo "  Aborting."
    exit 1
fi

echo ""
echo "  All services reachable. Running tests..."

# ---------------------------------------------------------------------------
# TEST 1: CREATE
# ---------------------------------------------------------------------------
print_header "TEST 1: CREATE — POST /org/v1/create"

ORG_PAYLOAD='{
  "name": "Karnataka Department of Horticulture",
  "type": "Government Department",
  "location": "locmap-000000000001",
  "department": "Horticulture",
  "address": "Lalbagh Main Road, Bengaluru 560004",
  "contactNumber": "+91-80-26578184",
  "contactEmail": "horticulture@karnataka.gov.in"
}'

CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${ORG_CREATE_URL}" \
    -H "Content-Type: application/json" \
    -d "${ORG_PAYLOAD}")

HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

echo "  HTTP Status: ${HTTP_CODE}"

if [ "$HTTP_CODE" = "200" ]; then
    print_result "pass" "CREATE returned HTTP 200"
else
    print_result "fail" "CREATE returned HTTP ${HTTP_CODE} (expected 200)"
    echo "  Response: ${RESPONSE_BODY}"
    echo ""
    echo "  Aborting remaining tests."
    exit 1
fi

# Extract the generated primary key (orgId in response result: org-<digits>)
ORG_ID=$(echo "$RESPONSE_BODY" | grep -o '"orgId":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -n "$ORG_ID" ]; then
    print_result "pass" "Got generated orgId: ${ORG_ID}"
else
    print_result "fail" "Could not extract orgId from response"
    echo "  Response was: ${RESPONSE_BODY}"
    echo "  Aborting remaining tests."
    exit 1
fi

echo ""
echo "  Waiting 2s for indexing..."
sleep 2

# ---------------------------------------------------------------------------
# TEST 2: POSTGRES VERIFICATION (via docker exec)
# ---------------------------------------------------------------------------
print_header "TEST 2: POSTGRES — Verify row in 'org' table"

PG_COUNT=$(docker exec "${PG_CONTAINER}" \
    psql -U "${PG_USER}" -d "${PG_DB}" -t -A \
    -c "SELECT count(*) FROM org WHERE org_id = '${ORG_ID}';" 2>/dev/null || echo "error")
PG_COUNT=$(echo "$PG_COUNT" | tr -d '[:space:]')

if [ "$PG_COUNT" = "1" ]; then
    print_result "pass" "Row found in Postgres (org table, org_id=${ORG_ID})"
else
    print_result "fail" "Row NOT found in Postgres (result=${PG_COUNT})"
fi

PG_STATUS=$(docker exec "${PG_CONTAINER}" \
    psql -U "${PG_USER}" -d "${PG_DB}" -t -A \
    -c "SELECT status FROM org WHERE org_id = '${ORG_ID}';" 2>/dev/null || echo "N/A")
echo "  Status in DB: $(echo "$PG_STATUS" | tr -d '[:space:]')"

# ---------------------------------------------------------------------------
# TEST 3: ELASTICSEARCH VERIFICATION
# ---------------------------------------------------------------------------
print_header "TEST 3: ELASTICSEARCH — Verify document in '${ES_INDEX}'"

ES_RESPONSE=$(curl -s "${ES_BASE}/${ES_INDEX}/_doc/${ORG_ID}" 2>/dev/null)
ES_FOUND=$(echo "$ES_RESPONSE" | grep -o '"found":[a-z]*' | cut -d: -f2)

if [ "$ES_FOUND" = "true" ]; then
    print_result "pass" "Document found in Elasticsearch (${ES_INDEX}/_doc/${ORG_ID})"
    echo "  Indexed fields:"
    echo "$ES_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    source = data.get('_source', {})
    for k in sorted(source.keys()):
        v = source[k]
        if isinstance(v, str) and len(v) > 50:
            v = v[:50] + '...'
        print(f'    {k}: {v}')
except: print('    (could not parse)')
" 2>/dev/null || echo "    (python3 not available for pretty-print)"
else
    print_result "fail" "Document NOT found in Elasticsearch"
    echo "  ES response: ${ES_RESPONSE}"
fi

# ---------------------------------------------------------------------------
# TEST 4: REDIS VERIFICATION (via docker exec)
# ---------------------------------------------------------------------------
print_header "TEST 4: REDIS — Verify cache entry"

REDIS_VALUE=$(docker exec "${REDIS_CONTAINER}" redis-cli GET "${ORG_ID}" 2>/dev/null)

if [ -n "$REDIS_VALUE" ] && [ "$REDIS_VALUE" != "(nil)" ]; then
    print_result "pass" "Cache entry found in Redis (key=${ORG_ID})"
    echo "  Value (first 200 chars): $(echo "$REDIS_VALUE" | head -c 200)"
else
    print_result "fail" "Cache entry NOT found in Redis (key=${ORG_ID})"
fi

REDIS_TTL=$(docker exec "${REDIS_CONTAINER}" redis-cli TTL "${ORG_ID}" 2>/dev/null)
echo "  TTL: ${REDIS_TTL}s"

# ---------------------------------------------------------------------------
# TEST 5: READ
# ---------------------------------------------------------------------------
print_header "TEST 5: READ — GET /org/v1/read/${ORG_ID}"

READ_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${ORG_READ_URL}/${ORG_ID}")
READ_HTTP=$(echo "$READ_RESPONSE" | tail -n1)
READ_BODY=$(echo "$READ_RESPONSE" | sed '$d')

echo "  HTTP Status: ${READ_HTTP}"

if [ "$READ_HTTP" = "200" ]; then
    print_result "pass" "READ returned HTTP 200"
else
    print_result "fail" "READ returned HTTP ${READ_HTTP} (expected 200)"
fi

if echo "$READ_BODY" | grep -q "Horticulture"; then
    print_result "pass" "READ response contains 'Horticulture'"
else
    print_result "fail" "READ response does not contain expected data"
    echo "  Response: ${READ_BODY}"
fi

# ---------------------------------------------------------------------------
# TEST 6: SEARCH by filter (department = Horticulture)
# ---------------------------------------------------------------------------
print_header "TEST 6: SEARCH — Filter by department=Horticulture"

SEARCH_PAYLOAD='{
  "filterCriteriaMap": {
    "department": "Horticulture"
  },
  "pageNumber": 0,
  "pageSize": 10
}'

SEARCH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${ORG_SEARCH_URL}" \
    -H "Content-Type: application/json" \
    -d "${SEARCH_PAYLOAD}")

SEARCH_HTTP=$(echo "$SEARCH_RESPONSE" | tail -n1)
SEARCH_BODY=$(echo "$SEARCH_RESPONSE" | sed '$d')

echo "  HTTP Status: ${SEARCH_HTTP}"

if [ "$SEARCH_HTTP" = "200" ]; then
    print_result "pass" "SEARCH returned HTTP 200"
else
    print_result "fail" "SEARCH returned HTTP ${SEARCH_HTTP} (expected 200)"
fi

if echo "$SEARCH_BODY" | grep -q "Horticulture"; then
    print_result "pass" "SEARCH results contain 'Horticulture'"
else
    print_result "fail" "SEARCH results do not contain the created org"
    echo "  Response (first 500 chars): $(echo "$SEARCH_BODY" | head -c 500)"
fi

# ---------------------------------------------------------------------------
# TEST 7: DELETE (soft delete)
# ---------------------------------------------------------------------------
print_header "TEST 7: DELETE — DELETE /org/v1/delete/${ORG_ID}"

DELETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "${ORG_DELETE_URL}/${ORG_ID}")
DELETE_HTTP=$(echo "$DELETE_RESPONSE" | tail -n1)

echo "  HTTP Status: ${DELETE_HTTP}"

if [ "$DELETE_HTTP" = "200" ]; then
    print_result "pass" "DELETE returned HTTP 200"
else
    print_result "fail" "DELETE returned HTTP ${DELETE_HTTP} (expected 200)"
fi

# Soft delete => status flips to INACTIVE in Postgres
PG_STATUS_AFTER=$(docker exec "${PG_CONTAINER}" \
    psql -U "${PG_USER}" -d "${PG_DB}" -t -A \
    -c "SELECT status FROM org WHERE org_id = '${ORG_ID}';" 2>/dev/null || echo "N/A")
PG_STATUS_AFTER=$(echo "$PG_STATUS_AFTER" | tr -d '[:space:]')

if [ "$PG_STATUS_AFTER" = "INACTIVE" ]; then
    print_result "pass" "Postgres row soft-deleted (status=INACTIVE)"
else
    print_result "fail" "Postgres status after delete is '${PG_STATUS_AFTER}' (expected INACTIVE)"
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
print_header "TEST SUMMARY"

echo "  Total:  ${TOTAL}"
echo "  Passed: ${PASSED}"
echo "  Failed: ${FAILED}"
echo ""

if [ "$FAILED" -eq 0 ]; then
    echo "  [${PASS}] ALL TESTS PASSED"
    echo ""
    exit 0
else
    echo "  [${FAIL}] SOME TESTS FAILED"
    echo ""
    exit 1
fi
