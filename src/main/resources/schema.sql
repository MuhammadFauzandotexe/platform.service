CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(32) NOT NULL,
    account_plan VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_accounts_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS whatsapp_sessions (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    session_name VARCHAR(255),
    account_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ,
    connected_at TIMESTAMPTZ,
    disconnected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_whatsapp_sessions_session_id UNIQUE (session_id),
    CONSTRAINT fk_whatsapp_sessions_account
        FOREIGN KEY (account_id) REFERENCES accounts (id)
);
