-- V3: request email tokens for secure Accept/Decline via email links
CREATE TABLE request_email_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(64) NOT NULL,
    request_id  BIGINT      NOT NULL,
    donor_id    BIGINT      NOT NULL,
    action      VARCHAR(20) NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    expiry_date DATETIME    NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_request_email_token UNIQUE (token),
    CONSTRAINT fk_token_request FOREIGN KEY (request_id) REFERENCES blood_requests (id),
    CONSTRAINT fk_token_donor FOREIGN KEY (donor_id) REFERENCES users (id),
    CONSTRAINT ck_token_action CHECK (action IN ('ACCEPT', 'DECLINE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_token_request ON request_email_tokens (request_id);
CREATE INDEX idx_token_donor ON request_email_tokens (donor_id);
