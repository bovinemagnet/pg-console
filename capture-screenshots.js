#!/usr/bin/env node
/**
 * Captures dark-mode screenshots of pg-console dashboards using Playwright.
 *
 * Usage:
 *   node capture-screenshots.js [--base-url URL] [--output-dir DIR]
 *
 * Defaults:
 *   --base-url   http://127.0.0.1:8080
 *   --output-dir docs/modules/user-guide/images
 */

const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

// Parse CLI arguments
function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    baseUrl: 'http://127.0.0.1:8080',
    outputDir: 'docs/modules/user-guide/images',
  };
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--base-url' && args[i + 1]) {
      opts.baseUrl = args[i + 1];
      i++;
    } else if (args[i] === '--output-dir' && args[i + 1]) {
      opts.outputDir = args[i + 1];
      i++;
    }
  }
  return opts;
}

// Dashboard routes to capture
const DASHBOARDS = [
  { route: '/', filename: 'screenshot-overview-dark.png', waitFor: '.card' },
  { route: '/slow-queries', filename: 'screenshot-slow-queries-dark.png', waitFor: 'table' },
  { route: '/activity', filename: 'screenshot-activity-dark.png', waitFor: 'table' },
  { route: '/locks', filename: 'screenshot-locks-dark.png', waitFor: '.card' },
  { route: '/tables', filename: 'screenshot-tables-dark.png', waitFor: 'table' },
  { route: '/databases', filename: 'screenshot-databases-dark.png', waitFor: 'table' },
  { route: '/diagnostics', filename: 'screenshot-diagnostics-index-dark.png', waitFor: '.card' },
  { route: '/diagnostics/metrics-history?instance=default', filename: 'screenshot-metrics-history-dark.png', waitFor: 'canvas' },
  { route: '/diagnostics/live-charts', filename: 'screenshot-live-charts-dark.png', waitFor: 'canvas' },
  { route: '/diagnostics/xid-wraparound', filename: 'screenshot-xid-wraparound-dark.png', waitFor: '.card' },
  { route: '/insights', filename: 'screenshot-insights-dark.png', waitFor: '.card' },
  { route: '/security', filename: 'screenshot-security-dark.png', waitFor: '.card' },
  { route: '/stopwatch', filename: 'screenshot-stopwatch-dark.png', waitFor: '.card' },
  { route: '/window-compare', filename: 'screenshot-window-compare-dark.png', waitFor: '.card' },
  { route: '/diagnostics/query-trends?instance=default', filename: 'screenshot-query-trends-dark.png', waitFor: '.card' },
  { route: '/diagnostics/database-trends?instance=default', filename: 'screenshot-database-trends-dark.png', waitFor: '.card' },
  { route: '/diagnostics/infrastructure-trends?instance=default', filename: 'screenshot-infrastructure-trends-dark.png', waitFor: '.card' },
];

async function main() {
  const opts = parseArgs();
  const outputDir = path.resolve(opts.outputDir);

  // Ensure output directory exists
  fs.mkdirSync(outputDir, { recursive: true });

  console.log(`Capturing screenshots from: ${opts.baseUrl}`);
  console.log(`Output directory: ${outputDir}`);
  console.log(`Dashboards to capture: ${DASHBOARDS.length}`);
  console.log('');

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 1,
  });

  const page = await context.newPage();

  // Set dark mode via localStorage before navigating
  // Navigate to base URL first to set the storage origin
  await page.goto(opts.baseUrl, { waitUntil: 'domcontentloaded', timeout: 15000 });
  await page.evaluate(() => {
    localStorage.setItem('pg-console-theme', 'dark');
    document.documentElement.setAttribute('data-bs-theme', 'dark');
  });

  let captured = 0;
  let failed = 0;

  for (const dashboard of DASHBOARDS) {
    const url = `${opts.baseUrl}${dashboard.route}`;
    const filepath = path.join(outputDir, dashboard.filename);

    try {
      process.stdout.write(`  Capturing ${dashboard.route} ... `);

      await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });

      // Ensure dark mode is applied
      await page.evaluate(() => {
        localStorage.setItem('pg-console-theme', 'dark');
        document.documentElement.setAttribute('data-bs-theme', 'dark');
      });

      // Wait for key content to appear
      try {
        await page.waitForSelector(dashboard.waitFor, { timeout: 10000 });
      } catch {
        // Content selector not found — page may have loaded differently, continue anyway
      }

      // Allow animations and charts to settle
      await page.waitForTimeout(1500);

      // Collapse the sidebar if it's expanded, to show more content
      const sidebarCollapsed = await page.evaluate(() => {
        return document.documentElement.classList.contains('sidebar-collapsed');
      });
      if (!sidebarCollapsed) {
        // Try clicking the sidebar toggle
        try {
          const toggle = await page.$('#sidebar-toggle, .sidebar-toggle, [data-action="toggle-sidebar"]');
          if (toggle) {
            await toggle.click();
            await page.waitForTimeout(500);
          }
        } catch {
          // No sidebar toggle found, that's fine
        }
      }

      await page.screenshot({ path: filepath, fullPage: false });
      console.log(`OK -> ${dashboard.filename}`);
      captured++;
    } catch (err) {
      console.log(`FAILED: ${err.message}`);
      failed++;
    }
  }

  await browser.close();

  console.log('');
  console.log(`Done: ${captured} captured, ${failed} failed out of ${DASHBOARDS.length} dashboards`);

  if (failed > 0) {
    process.exit(1);
  }
}

main().catch((err) => {
  console.error('Fatal error:', err.message);
  process.exit(1);
});
