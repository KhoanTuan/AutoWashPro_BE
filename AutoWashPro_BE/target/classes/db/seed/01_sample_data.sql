-- =============================================================================
-- AutoWashPro — Sample Data Seed (DML only, idempotent)
-- =============================================================================
-- Purpose : Populate empty / workflow tables for end-to-end testing (booking,
--           checkout, invoices, shift closeout, ledger, feedback).
-- Safety  : INSERT … ON CONFLICT DO NOTHING / WHERE NOT EXISTS only.
--           Does NOT alter schema, types, constraints, or existing rows.
--
-- Prerequisites (must exist — normally created by Spring Boot DatabaseSeeder):
--   loyalty_tier, role, staff, customer, vehicle, slot, service, service_variant
--
-- Demo references used below:
--   Customers  : 0902000001 (An), 0902000002 (Binh), 0902000003 (Cuong), …
--   Cashier    : manager  (has CASHIER_CHECKIN permission)
--   Technician : tech01
--   Services   : Basic Wash, Premium Wash, Full Detail
--
-- Run:
--   psql -U postgres -d autowashpro -f src/main/resources/db/seed/01_sample_data.sql
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 0. Optional dedicated cashier account (uses same bcrypt hash as manager demo)
-- ---------------------------------------------------------------------------
INSERT INTO staff (
    username, password_hash, full_name, email, phone_number,
    require_password_change, status, work_status,
    performance_kpi, total_jobs_completed, service_rating,
    created_at, updated_at
)
SELECT
    'cashier01',
    m.password_hash,
    'Nguyen Thi Cashier',
    'cashier01@autowashpro.com',
    '0901000099',
    FALSE,
    'ACTIVE',
    'IDLE',
    93.5,
    412,
    4.85,
    NOW(),
    NOW()
FROM staff m
WHERE m.username = 'manager'
  AND NOT EXISTS (SELECT 1 FROM staff WHERE username = 'cashier01');

INSERT INTO staff_role (staff_id, role_id)
SELECT s.staff_id, r.role_id
FROM staff s
JOIN role r ON r.role_name = 'ROLE_CASHIER'
WHERE s.username = 'cashier01'
  AND NOT EXISTS (
      SELECT 1 FROM staff_role sr
      WHERE sr.staff_id = s.staff_id AND sr.role_id = r.role_id
  );

-- ---------------------------------------------------------------------------
-- 1. Promotions (marketing / rescue vouchers)
-- ---------------------------------------------------------------------------
INSERT INTO promotion (
    promotion_code, name, description, discount_type, discount_value,
    valid_from, valid_to, status, is_rescue_voucher, created_at, updated_at
) VALUES
    ('WELCOME10',  'Welcome 10% Off',       'First-time walk-in discount',           'PERCENT', 10.00,  NOW() - INTERVAL '30 days', NOW() + INTERVAL '60 days',  'ACTIVE', FALSE, NOW(), NOW()),
    ('SILVER15',   'Silver Member 15%',     'Tier-based loyalty reward',             'PERCENT', 15.00,  NOW() - INTERVAL '7 days',  NOW() + INTERVAL '90 days',  'ACTIVE', FALSE, NOW(), NOW()),
    ('FLAT50K',    'Flat 50K VND Off',      'Weekday morning promotion',             'FIXED',   50000.00, NOW() - INTERVAL '1 day',   NOW() + INTERVAL '30 days',  'ACTIVE', FALSE, NOW(), NOW()),
    ('RESCUE-A001','RFM Rescue — Nguyen An', 'Automated win-back voucher',            'PERCENT', 15.00,  NOW() - INTERVAL '3 days',  NOW() + INTERVAL '27 days',  'ACTIVE', TRUE,  NOW(), NOW()),
    ('RESCUE-B002','RFM Rescue — Hoang Em',  'Automated win-back voucher',            'PERCENT', 15.00,  NOW() - INTERVAL '3 days',  NOW() + INTERVAL '27 days',  'ACTIVE', TRUE,  NOW(), NOW()),
    ('EXPIRED5',   'Expired Sample Promo',   'Should not apply — expired reference',  'PERCENT', 5.00,   NOW() - INTERVAL '90 days', NOW() - INTERVAL '30 days', 'EXPIRED', FALSE, NOW(), NOW())
ON CONFLICT (promotion_code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Customer ↔ Promotion assignments
-- ---------------------------------------------------------------------------
INSERT INTO customer_promotion (customer_id, promotion_id, status, applied_at, redeemed_at, created_at, updated_at)
SELECT c.customer_id, p.promotion_id, 'AVAILABLE', NULL, NULL, NOW(), NOW()
FROM customer c
JOIN promotion p ON p.promotion_code = 'WELCOME10'
WHERE c.phone_number = '0902000007'
  AND NOT EXISTS (
      SELECT 1 FROM customer_promotion cp
      WHERE cp.customer_id = c.customer_id AND cp.promotion_id = p.promotion_id
  );

INSERT INTO customer_promotion (customer_id, promotion_id, status, applied_at, redeemed_at, created_at, updated_at)
SELECT c.customer_id, p.promotion_id, 'AVAILABLE', NULL, NULL, NOW(), NOW()
FROM customer c
JOIN promotion p ON p.promotion_code = 'SILVER15'
WHERE c.phone_number = '0902000002'
  AND NOT EXISTS (
      SELECT 1 FROM customer_promotion cp
      WHERE cp.customer_id = c.customer_id AND cp.promotion_id = p.promotion_id
  );

INSERT INTO customer_promotion (customer_id, promotion_id, status, applied_at, redeemed_at, created_at, updated_at)
SELECT c.customer_id, p.promotion_id, 'REDEEMED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', NOW(), NOW()
FROM customer c
JOIN promotion p ON p.promotion_code = 'FLAT50K'
WHERE c.phone_number = '0902000001'
  AND NOT EXISTS (
      SELECT 1 FROM customer_promotion cp
      WHERE cp.customer_id = c.customer_id AND cp.promotion_id = p.promotion_id
  );

INSERT INTO customer_promotion (customer_id, promotion_id, status, applied_at, redeemed_at, created_at, updated_at)
SELECT c.customer_id, p.promotion_id, 'AVAILABLE', NULL, NULL, NOW(), NOW()
FROM customer c
JOIN promotion p ON p.promotion_code = 'RESCUE-A001'
WHERE c.phone_number = '0902000001'
  AND NOT EXISTS (
      SELECT 1 FROM customer_promotion cp
      WHERE cp.customer_id = c.customer_id AND cp.promotion_id = p.promotion_id
  );

INSERT INTO customer_promotion (customer_id, promotion_id, status, applied_at, redeemed_at, created_at, updated_at)
SELECT c.customer_id, p.promotion_id, 'AVAILABLE', NULL, NULL, NOW(), NOW()
FROM customer c
JOIN promotion p ON p.promotion_code = 'RESCUE-B002'
WHERE c.phone_number = '0902000005'
  AND NOT EXISTS (
      SELECT 1 FROM customer_promotion cp
      WHERE cp.customer_id = c.customer_id AND cp.promotion_id = p.promotion_id
  );

-- ---------------------------------------------------------------------------
-- 3. Bookings (mixed lifecycle states for Flow-1 testing)
-- ---------------------------------------------------------------------------

-- 3a. Unpaid walk-in — ready for cashier checkout
INSERT INTO booking (
    booking_code, customer_id, vehicle_id, slot_id, booking_type, booking_date,
    booking_status, payment_status, notes, cashier_id,
    base_subtotal, vehicle_surcharge, finalized_total_price, check_in_time,
    created_at, updated_at
)
SELECT
    'AWB-SEED-001',
    c.customer_id,
    v.vehicle_id,
    (SELECT slot_id FROM slot ORDER BY start_time LIMIT 1),
    'WALKIN',
    CURRENT_DATE,
    'PENDING_PAYMENT',
    'UNPAID',
    'Seed: awaiting checkout at POS',
    NULL,
    NULL, NULL, NULL, NULL,
    NOW(), NOW()
FROM customer c
JOIN vehicle v ON v.customer_id = c.customer_id AND v.license_plate = '51A-12345'
WHERE c.phone_number = '0902000001'
ON CONFLICT (booking_code) DO NOTHING;

-- 3b. Paid + confirmed (cash) — invoice fully settled
INSERT INTO booking (
    booking_code, customer_id, vehicle_id, slot_id, booking_type, booking_date,
    booking_status, payment_status, notes, cashier_id,
    base_subtotal, vehicle_surcharge, finalized_total_price, check_in_time,
    created_at, updated_at
)
SELECT
    'AWB-SEED-002',
    c.customer_id,
    v.vehicle_id,
    (SELECT slot_id FROM slot ORDER BY start_time OFFSET 1 LIMIT 1),
    'WALKIN',
    CURRENT_DATE,
    'CONFIRMED',
    'PAID',
    'Seed: paid in full by cash',
    (SELECT staff_id FROM staff WHERE username = 'manager' LIMIT 1),
    250000.00, 0.00, 250000.00,
    NOW() - INTERVAL '45 minutes',
    NOW(), NOW()
FROM customer c
JOIN vehicle v ON v.customer_id = c.customer_id AND v.license_plate = '51B-67890'
WHERE c.phone_number = '0902000002'
ON CONFLICT (booking_code) DO NOTHING;

-- 3c. Paid via split payment (cash + MoMo) — processing in bay
INSERT INTO booking (
    booking_code, customer_id, vehicle_id, slot_id, booking_type, booking_date,
    booking_status, payment_status, notes, cashier_id,
    base_subtotal, vehicle_surcharge, finalized_total_price, check_in_time,
    created_at, updated_at
)
SELECT
    'AWB-SEED-003',
    c.customer_id,
    v.vehicle_id,
    (SELECT slot_id FROM slot ORDER BY start_time OFFSET 2 LIMIT 1),
    'APP_BOOKING',
    CURRENT_DATE,
    'PROCESSING',
    'PAID',
    'Seed: split-payment SUV premium wash',
    (SELECT staff_id FROM staff WHERE username = 'cashier01' LIMIT 1),
    450000.00, 90000.00, 540000.00,
    NOW() - INTERVAL '90 minutes',
    NOW(), NOW()
FROM customer c
JOIN vehicle v ON v.customer_id = c.customer_id AND v.license_plate = '30C-11223'
WHERE c.phone_number = '0902000003'
ON CONFLICT (booking_code) DO NOTHING;

-- 3d. Completed visit — eligible for feedback
INSERT INTO booking (
    booking_code, customer_id, vehicle_id, slot_id, booking_type, booking_date,
    booking_status, payment_status, notes, cashier_id,
    base_subtotal, vehicle_surcharge, finalized_total_price, check_in_time,
    created_at, updated_at
)
SELECT
    'AWB-SEED-004',
    c.customer_id,
    v.vehicle_id,
    (SELECT slot_id FROM slot ORDER BY start_time OFFSET 3 LIMIT 1),
    'WALKIN',
    CURRENT_DATE - 1,
    'COMPLETED',
    'PAID',
    'Seed: completed yesterday',
    (SELECT staff_id FROM staff WHERE username = 'manager' LIMIT 1),
    890000.00, 0.00, 890000.00,
    (CURRENT_DATE - 1)::timestamp + TIME '10:30',
    NOW(), NOW()
FROM customer c
JOIN vehicle v ON v.customer_id = c.customer_id AND v.license_plate = '43D-44556'
WHERE c.phone_number = '0902000004'
ON CONFLICT (booking_code) DO NOTHING;

-- 3e. Appointment pending payment (future slot today)
INSERT INTO booking (
    booking_code, customer_id, vehicle_id, slot_id, booking_type, booking_date,
    booking_status, payment_status, notes,
    created_at, updated_at
)
SELECT
    'AWB-SEED-005',
    c.customer_id,
    v.vehicle_id,
    (SELECT slot_id FROM slot ORDER BY start_time DESC LIMIT 1),
    'APP_BOOKING',
    CURRENT_DATE,
    'PENDING_PAYMENT',
    'UNPAID',
    'Seed: app booking awaiting MoMo payment',
    NOW(), NOW()
FROM customer c
JOIN vehicle v ON v.customer_id = c.customer_id AND v.license_plate = '59E-77889'
WHERE c.phone_number = '0902000005'
ON CONFLICT (booking_code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. Booking line items
-- ---------------------------------------------------------------------------
INSERT INTO booking_item (booking_id, variant_id, actual_price)
SELECT b.booking_id, sv.variant_id, sv.calculated_price
FROM booking b
JOIN customer c ON c.customer_id = b.customer_id AND c.phone_number = '0902000001'
JOIN vehicle v ON v.vehicle_id = b.vehicle_id AND v.license_plate = '51A-12345'
JOIN service s ON s.service_name = 'Basic Wash'
JOIN service_variant sv ON sv.service_id = s.service_id AND sv.car_type = 'SEDAN'
WHERE b.booking_code = 'AWB-SEED-001'
  AND NOT EXISTS (SELECT 1 FROM booking_item bi WHERE bi.booking_id = b.booking_id);

INSERT INTO booking_item (booking_id, variant_id, actual_price)
SELECT b.booking_id, sv.variant_id, 250000.00
FROM booking b
JOIN service s ON s.service_name = 'Basic Wash'
JOIN service_variant sv ON sv.service_id = s.service_id AND sv.car_type = 'SUV'
WHERE b.booking_code = 'AWB-SEED-002'
  AND NOT EXISTS (SELECT 1 FROM booking_item bi WHERE bi.booking_id = b.booking_id);

INSERT INTO booking_item (booking_id, variant_id, actual_price)
SELECT b.booking_id, sv.variant_id, 540000.00
FROM booking b
JOIN service s ON s.service_name = 'Premium Wash'
JOIN service_variant sv ON sv.service_id = s.service_id AND sv.car_type = 'SEDAN'
WHERE b.booking_code = 'AWB-SEED-003'
  AND NOT EXISTS (SELECT 1 FROM booking_item bi WHERE bi.booking_id = b.booking_id);

INSERT INTO booking_item (booking_id, variant_id, actual_price)
SELECT b.booking_id, sv.variant_id, 890000.00
FROM booking b
JOIN service s ON s.service_name = 'Full Detail'
JOIN service_variant sv ON sv.service_id = s.service_id AND sv.car_type = 'TRUCK'
WHERE b.booking_code = 'AWB-SEED-004'
  AND NOT EXISTS (SELECT 1 FROM booking_item bi WHERE bi.booking_id = b.booking_id);

INSERT INTO booking_item (booking_id, variant_id, actual_price)
SELECT b.booking_id, sv.variant_id, sv.calculated_price
FROM booking b
JOIN service s ON s.service_name = 'Basic Wash'
JOIN service_variant sv ON sv.service_id = s.service_id AND sv.car_type = 'SEDAN'
WHERE b.booking_code = 'AWB-SEED-005'
  AND NOT EXISTS (SELECT 1 FROM booking_item bi WHERE bi.booking_id = b.booking_id);

-- ---------------------------------------------------------------------------
-- 5. Invoices (finalized bills + split-payment states)
-- ---------------------------------------------------------------------------
INSERT INTO invoice (
    invoice_code, booking_id, customer_id, cashier_id,
    subtotal, vehicle_surcharge, promotion_discount, total_amount,
    amount_paid_cash, amount_paid_momo,
    payment_status, split_payment_status, invoice_status, notes,
    created_at, updated_at
)
SELECT
    'INV-SEED-002',
    b.booking_id,
    b.customer_id,
    b.cashier_id,
    250000.00, 0.00, 0.00, 250000.00,
    250000.00, 0.00,
    'PAID', 'CASH_ONLY', 'FINALIZED', 'Seed invoice — full cash',
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-002'
ON CONFLICT (invoice_code) DO NOTHING;

INSERT INTO invoice (
    invoice_code, booking_id, customer_id, cashier_id,
    subtotal, vehicle_surcharge, promotion_discount, total_amount,
    amount_paid_cash, amount_paid_momo,
    payment_status, split_payment_status, invoice_status, notes,
    created_at, updated_at
)
SELECT
    'INV-SEED-003',
    b.booking_id,
    b.customer_id,
    b.cashier_id,
    450000.00, 90000.00, 0.00, 540000.00,
    240000.00, 300000.00,
    'PAID', 'SPLIT', 'FINALIZED', 'Seed invoice — split cash + MoMo',
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-003'
ON CONFLICT (invoice_code) DO NOTHING;

INSERT INTO invoice (
    invoice_code, booking_id, customer_id, cashier_id,
    subtotal, vehicle_surcharge, promotion_discount, total_amount,
    amount_paid_cash, amount_paid_momo,
    payment_status, split_payment_status, invoice_status, notes,
    created_at, updated_at
)
SELECT
    'INV-SEED-004',
    b.booking_id,
    b.customer_id,
    b.cashier_id,
    890000.00, 0.00, 0.00, 890000.00,
    890000.00, 0.00,
    'PAID', 'CASH_ONLY', 'FINALIZED', 'Seed invoice — completed visit',
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-004'
ON CONFLICT (invoice_code) DO NOTHING;

INSERT INTO invoice (
    invoice_code, booking_id, customer_id, cashier_id,
    subtotal, vehicle_surcharge, promotion_discount, total_amount,
    amount_paid_cash, amount_paid_momo,
    payment_status, split_payment_status, invoice_status, notes,
    created_at, updated_at
)
SELECT
    'INV-SEED-005',
    b.booking_id,
    b.customer_id,
    (SELECT staff_id FROM staff WHERE username = 'manager' LIMIT 1),
    250000.00, 0.00, 0.00, 250000.00,
    0.00, 0.00,
    'UNPAID', 'NONE', 'FINALIZED', 'Seed invoice — MoMo pending',
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-005'
ON CONFLICT (invoice_code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 6. Payment transactions (cash + MoMo ledger entries)
-- ---------------------------------------------------------------------------
INSERT INTO payment_transaction (
    invoice_id, transaction_ref, payment_method, amount, status,
    momo_result_code, momo_trans_id, raw_response, created_at, updated_at
)
SELECT
    i.invoice_id,
    'CASH-SEED-002',
    'CASH',
    250000.00,
    'SUCCESS',
    NULL, NULL, NULL,
    NOW(), NOW()
FROM invoice i
WHERE i.invoice_code = 'INV-SEED-002'
ON CONFLICT (transaction_ref) DO NOTHING;

INSERT INTO payment_transaction (
    invoice_id, transaction_ref, payment_method, amount, status,
    momo_result_code, momo_trans_id, raw_response, created_at, updated_at
)
SELECT
    i.invoice_id,
    'CASH-SEED-003A',
    'CASH',
    240000.00,
    'SUCCESS',
    NULL, NULL, NULL,
    NOW(), NOW()
FROM invoice i
WHERE i.invoice_code = 'INV-SEED-003'
ON CONFLICT (transaction_ref) DO NOTHING;

INSERT INTO payment_transaction (
    invoice_id, transaction_ref, payment_method, amount, status,
    momo_result_code, momo_trans_id, raw_response, created_at, updated_at
)
SELECT
    i.invoice_id,
    'MOMO-SEED-003B',
    'MOMO',
    300000.00,
    'SUCCESS',
    0,
    'MOMO-TX-987654321',
    '{"resultCode":0,"message":"Success","orderId":"MOMO-SEED-003B"}',
    NOW(), NOW()
FROM invoice i
WHERE i.invoice_code = 'INV-SEED-003'
ON CONFLICT (transaction_ref) DO NOTHING;

INSERT INTO payment_transaction (
    invoice_id, transaction_ref, payment_method, amount, status,
    momo_result_code, momo_trans_id, raw_response, created_at, updated_at
)
SELECT
    i.invoice_id,
    'CASH-SEED-004',
    'CASH',
    890000.00,
    'SUCCESS',
    NULL, NULL, NULL,
    NOW(), NOW()
FROM invoice i
WHERE i.invoice_code = 'INV-SEED-004'
ON CONFLICT (transaction_ref) DO NOTHING;

INSERT INTO payment_transaction (
    invoice_id, transaction_ref, payment_method, amount, status,
    momo_result_code, momo_trans_id, raw_response, created_at, updated_at
)
SELECT
    i.invoice_id,
    'MOMO-SEED-005-PENDING',
    'MOMO',
    250000.00,
    'PENDING',
    NULL, NULL, NULL,
    NOW(), NOW()
FROM invoice i
WHERE i.invoice_code = 'INV-SEED-005'
ON CONFLICT (transaction_ref) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 7. Shift closures (open + closed + flagged discrepancy)
-- ---------------------------------------------------------------------------
INSERT INTO shift_closure (
    cashier_id, shift_date, opening_balance, expected_balance, actual_balance, variance,
    total_cash, total_momo, total_revenue, status, closed_at, notes,
    created_at, updated_at
)
SELECT
    s.staff_id,
    CURRENT_DATE - 1,
    500000.00,
    1780000.00,
    1778500.00,
    -1500.00,
    980000.00,
    800000.00,
    1780000.00,
    'FLAGGED',
    (CURRENT_DATE - 1)::timestamp + TIME '22:15',
    'Seed: minor cash variance flagged for review',
    NOW(), NOW()
FROM staff s
WHERE s.username = 'manager'
  AND NOT EXISTS (
      SELECT 1 FROM shift_closure sc
      WHERE sc.cashier_id = s.staff_id
        AND sc.shift_date = CURRENT_DATE - 1
        AND sc.notes LIKE 'Seed:%'
  );

INSERT INTO shift_closure (
    cashier_id, shift_date, opening_balance, expected_balance, actual_balance, variance,
    total_cash, total_momo, total_revenue, status, closed_at, notes,
    created_at, updated_at
)
SELECT
    s.staff_id,
    CURRENT_DATE,
    500000.00,
    NULL, NULL, NULL,
    490000.00,
    300000.00,
    790000.00,
    'OPEN',
    NULL,
    'Seed: active shift — open for POS testing',
    NOW(), NOW()
FROM staff s
WHERE s.username = 'cashier01'
  AND NOT EXISTS (
      SELECT 1 FROM shift_closure sc
      WHERE sc.cashier_id = s.staff_id
        AND sc.shift_date = CURRENT_DATE
        AND sc.status = 'OPEN'
  );

-- Fallback open shift for manager if cashier01 was not created
INSERT INTO shift_closure (
    cashier_id, shift_date, opening_balance,
    total_cash, total_momo, total_revenue, status, notes,
    created_at, updated_at
)
SELECT
    s.staff_id,
    CURRENT_DATE,
    500000.00,
    0.00, 0.00, 0.00,
    'OPEN',
    'Seed: active shift — open for POS testing',
    NOW(), NOW()
FROM staff s
WHERE s.username = 'manager'
  AND NOT EXISTS (
      SELECT 1 FROM shift_closure sc
      WHERE sc.shift_date = CURRENT_DATE AND sc.status = 'OPEN'
  );

-- ---------------------------------------------------------------------------
-- 8. Financial ledger (sealed daily balancing book)
-- ---------------------------------------------------------------------------
INSERT INTO financial_ledger (
    ledger_date, opening_balance, total_revenue, total_cash, total_momo,
    total_expenses, closing_balance, status, sealed_at, summary_notes,
    created_at, updated_at
)
VALUES
    (
        CURRENT_DATE - 2,
        1000000.00, 3250000.00, 2100000.00, 1150000.00,
        150000.00, 4100000.00,
        'SEALED',
        (CURRENT_DATE - 2)::timestamp + TIME '23:59',
        'Seed ledger — steady weekday revenue',
        NOW(), NOW()
    ),
    (
        CURRENT_DATE - 1,
        4100000.00, 1780000.00, 980000.00, 800000.00,
        50000.00, 5830000.00,
        'SEALED',
        (CURRENT_DATE - 1)::timestamp + TIME '23:59',
        'Seed ledger — flagged shift variance day',
        NOW(), NOW()
    )
ON CONFLICT (ledger_date) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 9. AI analytics reports (linked to sealed ledgers)
-- ---------------------------------------------------------------------------
INSERT INTO ai_analytics_report (
    ledger_id, analysis_date, summary_text, recommendations, model_used,
    created_at, updated_at
)
SELECT
    fl.ledger_id,
    fl.ledger_date,
    'Revenue mix is 55% cash / 45% MoMo. Peak hours 09:00–11:00 drive 40% of daily intake.',
    E'- Add express lane for prepaid app bookings\n- Review flagged shift on variance > 1,000 VND\n- Push SILVER15 promo to SUV segment',
    'gemini-2.0-flash',
    NOW(), NOW()
FROM financial_ledger fl
WHERE fl.ledger_date = CURRENT_DATE - 1
  AND NOT EXISTS (
      SELECT 1 FROM ai_analytics_report r WHERE r.analysis_date = fl.ledger_date
  );

INSERT INTO ai_analytics_report (
    ledger_id, analysis_date, summary_text, recommendations, model_used,
    created_at, updated_at
)
SELECT
    fl.ledger_id,
    fl.ledger_date,
    'Strong premium upsell on truck segment. MoMo adoption increased 8% week-over-week.',
    E'- Bundle Full Detail + interior for TRUCK owners\n- Incentivize MoMo split-payment at peak\n- Schedule extra bay tech Saturday mornings',
    'gemini-2.0-flash',
    NOW(), NOW()
FROM financial_ledger fl
WHERE fl.ledger_date = CURRENT_DATE - 2
  AND NOT EXISTS (
      SELECT 1 FROM ai_analytics_report r WHERE r.analysis_date = fl.ledger_date
  );

-- ---------------------------------------------------------------------------
-- 10. Customer feedback (post-service ratings)
-- ---------------------------------------------------------------------------
INSERT INTO feedback (customer_id, booking_id, rating, comment, created_at, updated_at)
SELECT
    b.customer_id,
    b.booking_id,
    5,
    'Excellent detailing — truck looks brand new!',
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-004'
  AND NOT EXISTS (
      SELECT 1 FROM feedback f WHERE f.booking_id = b.booking_id
  );

INSERT INTO feedback (customer_id, booking_id, rating, comment, created_at, updated_at)
SELECT
    c.customer_id,
    b.booking_id,
    4,
    'Good wash quality, slight wait at peak hour.',
    NOW(), NOW()
FROM booking b
JOIN customer c ON c.customer_id = b.customer_id AND c.phone_number = '0902000002'
WHERE b.booking_code = 'AWB-SEED-002'
  AND NOT EXISTS (
      SELECT 1 FROM feedback f WHERE f.booking_id = b.booking_id
  );

INSERT INTO feedback (customer_id, booking_id, rating, comment, created_at, updated_at)
SELECT
    c.customer_id,
    NULL,
    5,
    'Staff was friendly — general station feedback (no booking link).',
    NOW(), NOW()
FROM customer c
WHERE c.phone_number = '0902000008'
  AND NOT EXISTS (
      SELECT 1 FROM feedback f
      WHERE f.customer_id = c.customer_id AND f.booking_id IS NULL
  );

-- ---------------------------------------------------------------------------
-- 11. Waiting queue entries (paid bookings checked in)
-- ---------------------------------------------------------------------------
INSERT INTO waiting_queue (
    booking_id, queue_lane, queue_status, priority_score, check_in_time, lane_position,
    created_at, updated_at
)
SELECT
    b.booking_id,
    'WALK_IN',
    'WAITING',
    72.5,
    b.check_in_time,
    2,
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-002'
  AND NOT EXISTS (SELECT 1 FROM waiting_queue wq WHERE wq.booking_id = b.booking_id);

INSERT INTO waiting_queue (
    booking_id, queue_lane, queue_status, priority_score, check_in_time, lane_position,
    created_at, updated_at
)
SELECT
    b.booking_id,
    'APPOINTMENT',
    'IN_BAY',
    88.0,
    b.check_in_time,
    1,
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-003'
  AND NOT EXISTS (SELECT 1 FROM waiting_queue wq WHERE wq.booking_id = b.booking_id);

-- ---------------------------------------------------------------------------
-- 12. Task checklist (technician assignment for in-progress booking)
-- ---------------------------------------------------------------------------
INSERT INTO task_checklist (
    booking_id, technician_id, start_time, end_time, status,
    created_at, updated_at
)
SELECT
    b.booking_id,
    (SELECT staff_id FROM staff WHERE username = 'tech01' LIMIT 1),
    NOW() - INTERVAL '30 minutes',
    NULL,
    'PROCESSING',
    NOW(), NOW()
FROM booking b
WHERE b.booking_code = 'AWB-SEED-003'
  AND NOT EXISTS (SELECT 1 FROM task_checklist tc WHERE tc.booking_id = b.booking_id);

COMMIT;

-- ---------------------------------------------------------------------------
-- Verification (read-only — optional)
-- ---------------------------------------------------------------------------
-- SELECT 'promotion'            AS tbl, COUNT(*) FROM promotion;
-- SELECT 'customer_promotion'   AS tbl, COUNT(*) FROM customer_promotion;
-- SELECT 'booking'              AS tbl, COUNT(*) FROM booking WHERE booking_code LIKE 'AWB-SEED-%';
-- SELECT 'invoice'              AS tbl, COUNT(*) FROM invoice WHERE invoice_code LIKE 'INV-SEED-%';
-- SELECT 'payment_transaction'  AS tbl, COUNT(*) FROM payment_transaction WHERE transaction_ref LIKE '%SEED%';
-- SELECT 'shift_closure'        AS tbl, COUNT(*) FROM shift_closure WHERE notes LIKE 'Seed:%';
-- SELECT 'financial_ledger'     AS tbl, COUNT(*) FROM financial_ledger WHERE summary_notes LIKE 'Seed%';
-- SELECT 'feedback'             AS tbl, COUNT(*) FROM feedback;
-- SELECT 'waiting_queue'        AS tbl, COUNT(*) FROM waiting_queue;
