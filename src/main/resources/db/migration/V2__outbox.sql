CREATE TABLE outbox_events (
 id UUID PRIMARY KEY, claim_id UUID NOT NULL REFERENCES claims(id), event_type VARCHAR(255) NOT NULL,
 payload VARCHAR(8000) NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
 published_at TIMESTAMP WITH TIME ZONE, attempts INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_outbox_pending ON outbox_events(published_at, occurred_at);
