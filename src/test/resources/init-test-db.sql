-- =============================================================================
-- Test Database Initialisation Script
-- =============================================================================
-- This script is executed when the PostgreSQL test container starts.
-- It sets up the pg_stat_statements extension and creates test data.

-- Enable pg_stat_statements extension for slow query testing
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Create the pgconsole schema (Flyway will handle the tables)
CREATE SCHEMA IF NOT EXISTS pgconsole;

-- =============================================================================
-- Create test tables for query testing
-- =============================================================================

-- Users table for testing
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Orders table for testing
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id),
    status VARCHAR(50) DEFAULT 'pending',
    total DECIMAL(10, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Products table for testing
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price DECIMAL(10, 2),
    stock INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =============================================================================
-- Insert test data
-- =============================================================================

-- Insert test users
INSERT INTO users (name, email) VALUES
    ('Test User 1', 'test1@example.com'),
    ('Test User 2', 'test2@example.com'),
    ('Test User 3', 'test3@example.com'),
    ('Test User 4', 'test4@example.com'),
    ('Test User 5', 'test5@example.com')
ON CONFLICT (email) DO NOTHING;

-- Insert test products
INSERT INTO products (name, price, stock) VALUES
    ('Product A', 29.99, 100),
    ('Product B', 49.99, 50),
    ('Product C', 99.99, 25),
    ('Product D', 149.99, 10),
    ('Product E', 199.99, 5)
ON CONFLICT DO NOTHING;

-- Insert test orders
INSERT INTO orders (user_id, status, total) VALUES
    (1, 'completed', 79.98),
    (1, 'pending', 149.99),
    (2, 'completed', 29.99),
    (3, 'shipped', 299.97),
    (4, 'pending', 49.99)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- Execute some queries to populate pg_stat_statements
-- =============================================================================

-- Run various queries to populate statistics
DO $$
BEGIN
    -- Simulate SELECT queries
    FOR i IN 1..10 LOOP
        PERFORM * FROM users WHERE id = i;
        PERFORM * FROM products WHERE price > 50;
        PERFORM COUNT(*) FROM orders;
    END LOOP;

    -- Simulate JOIN queries
    FOR i IN 1..5 LOOP
        PERFORM o.*, u.name
        FROM orders o
        JOIN users u ON o.user_id = u.id
        WHERE o.status = 'pending';
    END LOOP;
END $$;

-- =============================================================================
-- Create indexes for testing
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);

-- Create an intentionally unused index for index advisor testing
CREATE INDEX IF NOT EXISTS idx_users_created_unused ON users(created_at);

-- =============================================================================
-- Grant permissions
-- =============================================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO test;
GRANT ALL PRIVILEGES ON SCHEMA pgconsole TO test;
