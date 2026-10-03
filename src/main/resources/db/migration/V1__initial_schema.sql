CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_subject_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    picture_url TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paper_size VARCHAR(10) NOT NULL,
    color_mode VARCHAR(10) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uq_pricing_paper_color UNIQUE (paper_size, color_mode)
);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id UUID REFERENCES users(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PAYMENT_PENDING',
    payment_method VARCHAR(10) NOT NULL DEFAULT 'CASH',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_reference VARCHAR(255),
    payment_verified_at TIMESTAMPTZ,
    gateway_provider VARCHAR(50),
    gateway_order_id VARCHAR(255),
    gateway_payment_id VARCHAR(255) UNIQUE,
    payment_failure_reason TEXT,
    subtotal NUMERIC(10,2) NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(5) DEFAULT 'INR',
    version BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS file_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_key VARCHAR(500) UNIQUE NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_type VARCHAR(10) NOT NULL,
    file_size BIGINT NOT NULL,
    page_count INT DEFAULT 1,
    original_width INT,
    original_height INT,
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    file_asset_id UUID REFERENCES file_assets(id),
    print_ready_key VARCHAR(500),
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_type VARCHAR(10),
    file_size BIGINT,
    page_count INT DEFAULT 1,
    original_width INT,
    original_height INT,
    print_width INT,
    print_height INT,
    paper_size VARCHAR(10) NOT NULL,
    color_mode VARCHAR(10) NOT NULL,
    orientation VARCHAR(20) DEFAULT 'AUTO',
    copies INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(10,2) NOT NULL,
    total_price NUMERIC(10,2) NOT NULL,
    crop_data JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS print_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    order_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    attempt_count INT DEFAULT 0,
    agent_id VARCHAR(100),
    claim_token VARCHAR(255),
    claimed_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    printer_name VARCHAR(255),
    error_message TEXT,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL UNIQUE,
    p256dh TEXT NOT NULL,
    auth TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id VARCHAR(255),
    actor_type VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255),
    result VARCHAR(20) DEFAULT 'INFO',
    message TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_print_jobs_status ON print_jobs(status);
CREATE INDEX IF NOT EXISTS idx_print_jobs_order ON print_jobs(order_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at DESC);
