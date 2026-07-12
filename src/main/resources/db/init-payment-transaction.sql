-- MoMo Payment Gateway Integration - Database Migration
-- This script creates the payment_transaction table for tracking MoMo payment transactions

-- Create payment_transaction table
CREATE TABLE IF NOT EXISTS payment_transaction (
    transaction_id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    payment_gateway VARCHAR(50) NOT NULL,
    momo_trans_id VARCHAR(100),
    momo_request_id VARCHAR(100),
    momo_order_id VARCHAR(100),
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    result_code INTEGER,
    result_message TEXT,
    request_payload TEXT,
    response_payload TEXT,
    callback_payload TEXT,
    error_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_transaction_booking 
        FOREIGN KEY (booking_id) 
        REFERENCES booking(booking_id) 
        ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_payment_transaction_booking_id 
    ON payment_transaction(booking_id);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_momo_request_id 
    ON payment_transaction(momo_request_id);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_momo_trans_id 
    ON payment_transaction(momo_trans_id);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_status 
    ON payment_transaction(status);

CREATE INDEX IF NOT EXISTS idx_payment_transaction_created_at 
    ON payment_transaction(created_at);

-- Trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_payment_transaction_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_payment_transaction_update ON payment_transaction;
CREATE TRIGGER trigger_payment_transaction_update
    BEFORE UPDATE ON payment_transaction
    FOR EACH ROW
    EXECUTE FUNCTION update_payment_transaction_timestamp();
