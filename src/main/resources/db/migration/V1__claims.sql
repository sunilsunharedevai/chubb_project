CREATE TABLE claims (
 id UUID PRIMARY KEY, claim_number VARCHAR(40) NOT NULL UNIQUE,
 claim_type VARCHAR(20) NOT NULL CHECK (claim_type IN ('MOTOR','PROPERTY')),
 market VARCHAR(10) NOT NULL CHECK (market IN ('AU','NZ','SG','HK','MY','TH')),
 claimant_name VARCHAR(200) NOT NULL, incident_description VARCHAR(4000) NOT NULL,
 incident_date DATE NOT NULL, reported_at TIMESTAMP WITH TIME ZONE NOT NULL,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL, closed_at TIMESTAMP WITH TIME ZONE,
 status VARCHAR(40) NOT NULL CHECK (status IN ('SUBMITTED','ASSIGNED','UNDER_REVIEW','INFORMATION_REQUIRED','APPROVED','SETTLED','REJECTED')),
 assigned_officer_id VARCHAR(100), assigned_officer_name VARCHAR(200),
 estimated_liability NUMERIC(19,2) NOT NULL CHECK (estimated_liability >= 0),
 approved_settlement_amount NUMERIC(19,2) CHECK (approved_settlement_amount >= 0),
 decision_reason VARCHAR(2000), version BIGINT NOT NULL
);
CREATE INDEX idx_claim_status ON claims(status);
CREATE INDEX idx_claim_officer ON claims(assigned_officer_id);
CREATE INDEX idx_claim_status_officer ON claims(status, assigned_officer_id);
CREATE TABLE information_requests (
 id UUID PRIMARY KEY, claim_id UUID NOT NULL REFERENCES claims(id), question VARCHAR(255) NOT NULL,
 status VARCHAR(20) NOT NULL CHECK (status IN ('OPEN','PROVIDED')), response VARCHAR(4000),
 requested_at TIMESTAMP WITH TIME ZONE NOT NULL, provided_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_information_claim ON information_requests(claim_id);
CREATE TABLE claim_history (
 id UUID PRIMARY KEY, claim_id UUID NOT NULL REFERENCES claims(id), operation VARCHAR(255) NOT NULL,
 from_status VARCHAR(40), to_status VARCHAR(40) NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
 aggregate_version BIGINT NOT NULL, UNIQUE(claim_id, aggregate_version)
);
CREATE INDEX idx_history_claim_time ON claim_history(claim_id, occurred_at);
