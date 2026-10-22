CREATE TABLE dining_table (
  id BIGSERIAL PRIMARY KEY,
  table_number INT UNIQUE NOT NULL,
  qr_token VARCHAR(64) UNIQUE NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE category (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE menu_item (
  id BIGSERIAL PRIMARY KEY,
  category_id BIGINT NOT NULL REFERENCES category(id),
  name VARCHAR(120) NOT NULL,
  description TEXT,
  price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  image_url TEXT,
  veg BOOLEAN,
  available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  table_id BIGINT NOT NULL REFERENCES dining_table(id),
  status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  note TEXT,
  total NUMERIC(10,2) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_status ON orders(status, created_at);

CREATE TABLE order_item (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  menu_item_id BIGINT NOT NULL REFERENCES menu_item(id),
  quantity INT NOT NULL CHECK (quantity > 0),
  unit_price NUMERIC(10,2) NOT NULL
);

CREATE TABLE waiter_call (
  id BIGSERIAL PRIMARY KEY,
  table_id BIGINT NOT NULL REFERENCES dining_table(id),
  reason VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_waiter_status ON waiter_call(status, created_at);

CREATE TABLE staff_user (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(60) UNIQUE NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL
);
