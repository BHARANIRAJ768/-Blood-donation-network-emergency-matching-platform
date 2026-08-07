CREATE DATABASE blood_donation_platform;
USE blood_donation_platform;
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    role ENUM('DONOR', 'REQUESTER', 'ADMIN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE donors (
    donor_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    blood_group VARCHAR(5) NOT NULL,
    age INT NOT NULL,
    city VARCHAR(100),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    available BOOLEAN DEFAULT TRUE,
    last_donation_date DATE,
    CONSTRAINT fk_donor_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);
CREATE TABLE donations (
    donation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    donation_date DATE NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    units INT NOT NULL,
    donation_center VARCHAR(150),

    CONSTRAINT fk_donation_donor
        FOREIGN KEY (donor_id)
        REFERENCES donors(donor_id)
        ON DELETE CASCADE
);
CREATE TABLE hospitals (
    hospital_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_name VARCHAR(150) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    phone VARCHAR(15),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7)
);
CREATE TABLE emergency_requests (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    units_required INT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    status ENUM('OPEN', 'MATCHED', 'FULFILLED', 'CANCELLED') DEFAULT 'OPEN',
    required_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_user
        FOREIGN KEY (requester_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_request_hospital
        FOREIGN KEY (hospital_id)
        REFERENCES hospitals(hospital_id)
        ON DELETE CASCADE
);
CREATE TABLE matches (
    match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    match_score DECIMAL(5,2),
    distance_km DECIMAL(8,2),
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_request
        FOREIGN KEY (request_id)
        REFERENCES emergency_requests(request_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_match_donor
        FOREIGN KEY (donor_id)
        REFERENCES donors(donor_id)
        ON DELETE CASCADE,
    UNIQUE (request_id, donor_id)
);
CREATE TABLE match_responses (
    response_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id BIGINT NOT NULL UNIQUE,
    donor_id BIGINT NOT NULL,
    response ENUM('ACCEPTED', 'REJECTED') NOT NULL,
    responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_response_match
        FOREIGN KEY (match_id)
        REFERENCES matches(match_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_response_donor
        FOREIGN KEY (donor_id)
        REFERENCES donors(donor_id)
        ON DELETE CASCADE
);
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    request_id BIGINT,
    message VARCHAR(500) NOT NULL,
    type ENUM('EMERGENCY', 'MATCH', 'SYSTEM') NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_notification_request
        FOREIGN KEY (request_id)
        REFERENCES emergency_requests(request_id)
        ON DELETE SET NULL
);