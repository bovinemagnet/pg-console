#!/usr/bin/env bash
# capture-screenshots.sh — Capture dark-mode screenshots of pg-console dashboards.
#
# Usage:
#   ./capture-screenshots.sh [port] [host]
#
# Defaults:
#   port = 8080
#   host = 127.0.0.1
#
# Prerequisites:
#   - Node.js and npx on PATH
#   - pg-console running at the given host:port

set -euo pipefail

PORT="${1:-8080}"
HOST="${2:-127.0.0.1}"
BASE_URL="http://${HOST}:${PORT}"

USER_GUIDE_IMAGES="docs/modules/user-guide/images"
ROOT_IMAGES="docs/modules/ROOT/images"

# Colours for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No colour

echo -e "${YELLOW}pg-console screenshot capture${NC}"
echo "  Base URL : ${BASE_URL}"
echo ""

# --- Pre-flight checks ---

# 1. Node.js / npx
if ! command -v npx &>/dev/null; then
  echo -e "${RED}Error: npx not found. Install Node.js first.${NC}"
  exit 1
fi

# 2. App health check
echo -n "Checking app is running at ${BASE_URL} ... "
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/" 2>/dev/null || true)
if [ "${HTTP_CODE}" != "200" ]; then
  echo -e "${RED}FAILED (HTTP ${HTTP_CODE:-unreachable})${NC}"
  echo "Please start pg-console first, e.g.: gradle21w quarkusDev"
  exit 1
fi
echo -e "${GREEN}OK${NC}"

# --- Prepare output directories ---
mkdir -p "${USER_GUIDE_IMAGES}"
mkdir -p "${ROOT_IMAGES}"

# --- Install Playwright if needed ---
echo ""
if [ ! -d "node_modules/playwright" ]; then
  echo "Installing Playwright ..."
  npm install --no-save playwright 2>&1 | tail -1
fi
echo "Ensuring Playwright Chromium is installed ..."
npx playwright install chromium 2>&1 | grep -v "^$" || true

# --- Run the capture script ---
echo ""
echo "Capturing screenshots ..."
echo ""
node capture-screenshots.js --base-url "${BASE_URL}" --output-dir "${USER_GUIDE_IMAGES}"

# --- Copy hero screenshot to ROOT module ---
HERO="screenshot-overview-dark.png"
if [ -f "${USER_GUIDE_IMAGES}/${HERO}" ]; then
  cp "${USER_GUIDE_IMAGES}/${HERO}" "${ROOT_IMAGES}/${HERO}"
  echo ""
  echo -e "Copied hero screenshot to ${ROOT_IMAGES}/${HERO}"
fi

# --- Report ---
echo ""
echo -e "${GREEN}Screenshot capture complete!${NC}"
echo ""
echo "Files in ${USER_GUIDE_IMAGES}/:"
ls -lh "${USER_GUIDE_IMAGES}/"*.png 2>/dev/null || echo "  (none)"
echo ""
echo "Files in ${ROOT_IMAGES}/:"
ls -lh "${ROOT_IMAGES}/"*.png 2>/dev/null || echo "  (none)"
