CREATE TABLE ORDERS(
    id UUID PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL ,
    status VARCHAR(20) NOT NULL ,
    total_amount DECIMAL(10, 2) NOT NULL ,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE ORDER_ITEMS(
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES ORDERS(id),
    product_name VARCHAR(50) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

ALTER TABLE ORDERS
ADD CONSTRAINT check_dates
CHECK (created_at <= updated_at);

CREATE INDEX idx_orders_status ON ORDERS(status);
CREATE INDEX idx_orders_customer ON ORDERS(customer_id);