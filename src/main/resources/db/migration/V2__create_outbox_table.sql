CREATE TABLE outbox(
    id UUID NOT NULL PRIMARY KEY,
    topic TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE index outbox_created_at on outbox(created_at);