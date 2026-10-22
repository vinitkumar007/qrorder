CREATE TABLE app_setting (
  setting_key VARCHAR(60) PRIMARY KEY,
  setting_value VARCHAR(200) NOT NULL
);
INSERT INTO app_setting (setting_key, setting_value) VALUES
  ('restaurant_name', 'Hotel Rasoi'),
  ('gst_percent', '5'),
  ('service_percent', '0');

CREATE TABLE payment (
  id BIGSERIAL PRIMARY KEY,
  table_id BIGINT NOT NULL REFERENCES dining_table(id),
  subtotal NUMERIC(10,2) NOT NULL,
  gst_percent NUMERIC(5,2) NOT NULL,
  tax NUMERIC(10,2) NOT NULL,
  service_percent NUMERIC(5,2) NOT NULL,
  service NUMERIC(10,2) NOT NULL,
  total NUMERIC(10,2) NOT NULL,
  method VARCHAR(10) NOT NULL,
  cashier VARCHAR(60) NOT NULL,
  paid_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_payment_paid_at ON payment(paid_at);

ALTER TABLE orders ADD COLUMN payment_id BIGINT REFERENCES payment(id);
CREATE INDEX idx_orders_unpaid ON orders(table_id) WHERE payment_id IS NULL;
