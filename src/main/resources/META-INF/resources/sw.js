/**
 * PostgreSQL Console Service Worker
 * Provides offline capability and caching for the dashboard shell
 *
 * @author Paul Snow
 * @version 0.0.0
 */

const CACHE_NAME = 'pg-console-v1';
const SHELL_CACHE = 'pg-console-shell-v1';
const DATA_CACHE = 'pg-console-data-v1';

// Static assets to cache for the app shell
const SHELL_ASSETS = [
    '/',
    '/manifest.json',
    // Bootstrap CSS (CDN - cache for offline)
    'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css',
    // Bootstrap Icons (CDN)
    'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css',
    // Bootstrap JS (CDN)
    'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js',
    // HTMX (CDN)
    'https://unpkg.com/htmx.org@2.0.4'
];

// Pages to cache for offline access
const PAGE_ROUTES = [
    '/',
    '/activity',
    '/slow-queries',
    '/locks',
    '/tables',
    '/indexes',
    '/vacuum',
    '/replication',
    '/extensions',
    '/settings',
    '/logical-replication',
    '/cdc',
    '/data-lineage',
    '/partitions'
];

/**
 * Install event - cache the app shell
 */
self.addEventListener('install', (event) => {
    console.log('[ServiceWorker] Installing...');

    event.waitUntil(
        caches.open(SHELL_CACHE)
            .then((cache) => {
                console.log('[ServiceWorker] Caching app shell');
                // Cache shell assets, but don't fail install if CDN assets fail
                return Promise.allSettled(
                    SHELL_ASSETS.map(url =>
                        cache.add(url).catch(err => {
                            console.warn(`[ServiceWorker] Failed to cache: ${url}`, err);
                        })
                    )
                );
            })
            .then(() => {
                console.log('[ServiceWorker] App shell cached');
                return self.skipWaiting();
            })
    );
});

/**
 * Activate event - clean up old caches
 */
self.addEventListener('activate', (event) => {
    console.log('[ServiceWorker] Activating...');

    event.waitUntil(
        caches.keys()
            .then((cacheNames) => {
                return Promise.all(
                    cacheNames
                        .filter((cacheName) => {
                            // Remove old cache versions
                            return cacheName.startsWith('pg-console-') &&
                                   cacheName !== SHELL_CACHE &&
                                   cacheName !== DATA_CACHE;
                        })
                        .map((cacheName) => {
                            console.log('[ServiceWorker] Removing old cache:', cacheName);
                            return caches.delete(cacheName);
                        })
                );
            })
            .then(() => {
                console.log('[ServiceWorker] Claiming clients');
                return self.clients.claim();
            })
    );
});

/**
 * Fetch event - serve from cache, fallback to network
 * Uses stale-while-revalidate strategy for pages
 * Uses cache-first for static assets
 */
self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    // Skip non-GET requests
    if (event.request.method !== 'GET') {
        return;
    }

    // Skip WebSocket and htmx SSE connections
    if (url.protocol === 'ws:' || url.protocol === 'wss:') {
        return;
    }

    // Skip API data requests (always fetch fresh)
    if (url.pathname.startsWith('/api/')) {
        event.respondWith(networkFirst(event.request));
        return;
    }

    // For page routes - use stale-while-revalidate
    if (isPageRoute(url.pathname)) {
        event.respondWith(staleWhileRevalidate(event.request));
        return;
    }

    // For static assets (CSS, JS, images) - cache first
    if (isStaticAsset(url)) {
        event.respondWith(cacheFirst(event.request));
        return;
    }

    // Default: network first with cache fallback
    event.respondWith(networkFirst(event.request));
});

/**
 * Check if the path is a known page route
 */
function isPageRoute(pathname) {
    return PAGE_ROUTES.includes(pathname) ||
           PAGE_ROUTES.some(route => pathname.startsWith(route + '?'));
}

/**
 * Check if the URL is a static asset
 */
function isStaticAsset(url) {
    const staticExtensions = ['.css', '.js', '.png', '.jpg', '.jpeg', '.gif', '.svg', '.woff', '.woff2', '.ttf', '.eot'];
    return staticExtensions.some(ext => url.pathname.endsWith(ext)) ||
           url.hostname.includes('cdn.jsdelivr.net') ||
           url.hostname.includes('unpkg.com');
}

/**
 * Cache-first strategy: try cache, then network
 */
async function cacheFirst(request) {
    const cached = await caches.match(request);
    if (cached) {
        return cached;
    }

    try {
        const response = await fetch(request);
        if (response.ok) {
            const cache = await caches.open(SHELL_CACHE);
            cache.put(request, response.clone());
        }
        return response;
    } catch (error) {
        console.warn('[ServiceWorker] Network request failed:', request.url);
        return new Response('Offline', { status: 503 });
    }
}

/**
 * Network-first strategy: try network, then cache
 */
async function networkFirst(request) {
    try {
        const response = await fetch(request);
        if (response.ok) {
            const cache = await caches.open(DATA_CACHE);
            cache.put(request, response.clone());
        }
        return response;
    } catch (error) {
        const cached = await caches.match(request);
        if (cached) {
            return cached;
        }
        return offlineFallback(request);
    }
}

/**
 * Stale-while-revalidate: return cache immediately, update in background
 */
async function staleWhileRevalidate(request) {
    const cache = await caches.open(DATA_CACHE);
    const cached = await cache.match(request);

    // Fetch from network in background
    const networkPromise = fetch(request)
        .then((response) => {
            if (response.ok) {
                cache.put(request, response.clone());
            }
            return response;
        })
        .catch((error) => {
            console.warn('[ServiceWorker] Background fetch failed:', request.url);
            return null;
        });

    // Return cached version immediately if available
    if (cached) {
        return cached;
    }

    // Otherwise wait for network
    const networkResponse = await networkPromise;
    if (networkResponse) {
        return networkResponse;
    }

    return offlineFallback(request);
}

/**
 * Return an offline fallback page
 */
function offlineFallback(request) {
    const url = new URL(request.url);

    // For page requests, return a simple offline HTML
    if (request.headers.get('Accept')?.includes('text/html')) {
        return new Response(`
            <!DOCTYPE html>
            <html lang="en" data-bs-theme="dark">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Offline - PostgreSQL Console</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: #1a1d21;
                        color: #e9ecef;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        margin: 0;
                        text-align: center;
                    }
                    .offline-container {
                        padding: 2rem;
                    }
                    .offline-icon {
                        font-size: 4rem;
                        margin-bottom: 1rem;
                    }
                    h1 {
                        color: #0d6efd;
                        margin-bottom: 0.5rem;
                    }
                    p {
                        color: #6c757d;
                        margin-bottom: 1.5rem;
                    }
                    button {
                        background: #0d6efd;
                        color: white;
                        border: none;
                        padding: 0.75rem 1.5rem;
                        border-radius: 0.375rem;
                        cursor: pointer;
                        font-size: 1rem;
                    }
                    button:hover {
                        background: #0b5ed7;
                    }
                </style>
            </head>
            <body>
                <div class="offline-container">
                    <div class="offline-icon">📡</div>
                    <h1>You're Offline</h1>
                    <p>Unable to connect to the PostgreSQL Console. Please check your network connection.</p>
                    <button onclick="window.location.reload()">Try Again</button>
                </div>
            </body>
            </html>
        `, {
            status: 503,
            headers: { 'Content-Type': 'text/html' }
        });
    }

    // For other requests
    return new Response('Offline', { status: 503 });
}

/**
 * Handle background sync for offline actions
 */
self.addEventListener('sync', (event) => {
    console.log('[ServiceWorker] Sync event:', event.tag);

    if (event.tag === 'refresh-dashboard') {
        event.waitUntil(
            // Refresh cached dashboard data
            caches.open(DATA_CACHE)
                .then((cache) => {
                    return Promise.all(
                        PAGE_ROUTES.map(route =>
                            fetch(route)
                                .then(response => cache.put(route, response))
                                .catch(err => console.warn('Sync failed for:', route))
                        )
                    );
                })
        );
    }
});

/**
 * Handle push notifications (future feature)
 */
self.addEventListener('push', (event) => {
    if (!event.data) return;

    const data = event.data.json();

    event.waitUntil(
        self.registration.showNotification(data.title || 'PostgreSQL Console', {
            body: data.body || 'New alert',
            icon: '/icons/icon-192.png',
            badge: '/icons/icon-72.png',
            tag: data.tag || 'pg-console-notification',
            data: data.url || '/'
        })
    );
});

/**
 * Handle notification clicks
 */
self.addEventListener('notificationclick', (event) => {
    event.notification.close();

    event.waitUntil(
        clients.matchAll({ type: 'window' })
            .then((clientList) => {
                // Focus existing window if available
                for (const client of clientList) {
                    if (client.url === event.notification.data && 'focus' in client) {
                        return client.focus();
                    }
                }
                // Open new window
                if (clients.openWindow) {
                    return clients.openWindow(event.notification.data);
                }
            })
    );
});

console.log('[ServiceWorker] Script loaded');
