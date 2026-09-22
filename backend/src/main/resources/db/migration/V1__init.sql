-- =========================================================================
-- VitalDrop - Every Drop is Vital
-- V1: initial schema
-- =========================================================================

CREATE TABLE users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(120)  NOT NULL,
    email               VARCHAR(190)  NOT NULL,
    phone               VARCHAR(30)   NOT NULL,
    password            VARCHAR(100)  NOT NULL,
    role                VARCHAR(20)   NOT NULL,
    city                VARCHAR(100)  NULL,
    district            VARCHAR(100)  NULL,
    blood_group         VARCHAR(5)    NULL,
    available           BOOLEAN       NOT NULL DEFAULT FALSE,
    last_donation_date  DATE          NULL,
    latitude            DOUBLE        NULL,
    longitude           DOUBLE        NULL,
    photo               MEDIUMTEXT    NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_role CHECK (role IN ('DONOR', 'PATIENT', 'ADMIN')),
    CONSTRAINT ck_users_blood_group CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_blood_group ON users (blood_group);
CREATE INDEX idx_users_available ON users (available);
CREATE INDEX idx_users_city ON users (city);
CREATE INDEX idx_users_district ON users (district);

CREATE TABLE blood_requests (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id          BIGINT       NOT NULL,
    donor_id            BIGINT       NULL,
    blood_group         VARCHAR(5)   NOT NULL,
    hospital_name       VARCHAR(200) NOT NULL,
    hospital_address    VARCHAR(300) NULL,
    patient_name        VARCHAR(120) NOT NULL,
    phone               VARCHAR(30)  NOT NULL,
    units               INT          NOT NULL DEFAULT 1,
    reason              VARCHAR(500) NULL,
    emergency           BOOLEAN      NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    declined_by         VARCHAR(255) NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at         TIMESTAMP    NULL,
    completed_at        TIMESTAMP    NULL,
    CONSTRAINT fk_blood_request_patient FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT fk_blood_request_donor   FOREIGN KEY (donor_id)   REFERENCES users (id),
    CONSTRAINT ck_blood_request_group   CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    CONSTRAINT ck_blood_request_status  CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_blood_request_units   CHECK (units >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_blood_request_patient ON blood_requests (patient_id);
CREATE INDEX idx_blood_request_donor   ON blood_requests (donor_id);
CREATE INDEX idx_blood_request_status  ON blood_requests (status);
CREATE INDEX idx_blood_request_group   ON blood_requests (blood_group);

CREATE TABLE donation_history (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id          BIGINT       NOT NULL,
    patient_id        BIGINT       NOT NULL,
    blood_request_id  BIGINT       NOT NULL,
    blood_group       VARCHAR(5)   NOT NULL,
    hospital_name     VARCHAR(200) NULL,
    donation_date     DATE         NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donation_donor   FOREIGN KEY (donor_id)          REFERENCES users (id),
    CONSTRAINT fk_donation_patient FOREIGN KEY (patient_id)        REFERENCES users (id),
    CONSTRAINT fk_donation_request FOREIGN KEY (blood_request_id)  REFERENCES blood_requests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_donation_donor   ON donation_history (donor_id);
CREATE INDEX idx_donation_patient ON donation_history (patient_id);

CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     TEXT         NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    request_id  BIGINT       NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_receiver ON notifications (receiver_id, is_read);

CREATE TABLE password_reset_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(64) NOT NULL,
    user_id     BIGINT      NOT NULL,
    expiry_date DATETIME    NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reset_token UNIQUE (token),
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reset_user ON password_reset_tokens (user_id);
