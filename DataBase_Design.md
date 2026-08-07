# 🩸 Blood Donation Network & Emergency Matching Platform

# Database Design

The system uses a relational MySQL database to store users, donors,
blood donation history, hospitals, emergency requests, donor matching,
responses, and notifications.

---

## 1. Users Table

### Purpose
Stores the basic account details of all users.

| Column | Type | Key |
|---|---|---|
| user_id | BIGINT | PK |
| name | VARCHAR(100) | |
| email | VARCHAR(150) | UNIQUE |
| password | VARCHAR(255) | |
| phone | VARCHAR(15) | |
| role | VARCHAR(20) | |

### Used For
- Login and registration
- User identification
- Role management
- Contact information

---

## 2. Donors Table

### Purpose
Stores information related specifically to blood donors.

| Column | Type | Key |
|---|---|---|
| donor_id | BIGINT | PK |
| user_id | BIGINT | FK |
| blood_group | VARCHAR(5) | |
| age | INT | |
| city | VARCHAR(100) | |
| latitude | DECIMAL(10,7) | |
| longitude | DECIMAL(10,7) | |
| available | BOOLEAN | |
| last_donation_date | DATE | |

### Used For
- Storing blood group
- Checking donor availability
- Finding nearby donors
- Checking last donation date
- Matching donors with requests

---

## 3. Donations Table

### Purpose
Stores the donation history of donors.

| Column | Type | Key |
|---|---|---|
| donation_id | BIGINT | PK |
| donor_id | BIGINT | FK |
| donation_date | DATE | |
| blood_group | VARCHAR(5) | |
| units | INT | |
| donation_center | VARCHAR(150) | |

### Used For
- Maintaining donation history
- Tracking previous donations
- Displaying donor activity

---

## 4. Hospitals Table

### Purpose
Stores hospital information where blood is required.

| Column | Type | Key |
|---|---|---|
| hospital_id | BIGINT | PK |
| hospital_name | VARCHAR(150) | |
| address | VARCHAR(255) | |
| city | VARCHAR(100) | |
| phone | VARCHAR(15) | |
| latitude | DECIMAL(10,7) | |
| longitude | DECIMAL(10,7) | |

### Used For
- Storing hospital details
- Identifying request location
- Calculating distance from donor to hospital

---

## 5. Emergency Requests Table

### Purpose
Stores emergency blood requirements created by requesters.

| Column | Type | Key |
|---|---|---|
| request_id | BIGINT | PK |
| requester_id | BIGINT | FK |
| hospital_id | BIGINT | FK |
| blood_group | VARCHAR(5) | |
| units_required | INT | |
| priority | VARCHAR(20) | |
| status | VARCHAR(20) | |
| required_date | DATE | |
| created_at | TIMESTAMP | |

### Used For
- Creating blood requests
- Storing required blood group
- Storing required units
- Managing emergency priority
- Tracking request status

---

## 6. Matches Table ⭐

### Purpose
Stores the result of matching donors with emergency requests.

| Column | Type | Key |
|---|---|---|
| match_id | BIGINT | PK |
| request_id | BIGINT | FK |
| donor_id | BIGINT | FK |
| match_score | DECIMAL(5,2) | |
| distance_km | DECIMAL(8,2) | |
| status | VARCHAR(20) | |
| created_at | TIMESTAMP | |

### Used For
- Connecting donors with emergency requests
- Storing matching score
- Storing donor distance
- Ranking suitable donors
- Tracking match status

> **This is one of the most important tables in the project because it supports the Emergency Matching Engine.**

---

## 7. Match Responses Table

### Purpose
Stores the donor's response to a matching request.

| Column | Type | Key |
|---|---|---|
| response_id | BIGINT | PK |
| match_id | BIGINT | FK |
| donor_id | BIGINT | FK |
| response | VARCHAR(20) | |
| responded_at | TIMESTAMP | |

### Used For
- Recording donor response
- Accepting a request
- Rejecting a request
- Tracking response time

---

## 8. Notifications Table

### Purpose
Stores notifications sent to users.

| Column | Type | Key |
|---|---|---|
| notification_id | BIGINT | PK |
| user_id | BIGINT | FK |
| request_id | BIGINT | FK |
| message | VARCHAR(500) | |
| type | VARCHAR(50) | |
| is_read | BOOLEAN | |
| created_at | TIMESTAMP | |

### Used For
- Emergency alerts
- Donor match notifications
- Request status updates
- System notifications

---

# 🔗 Database Relationships

```text
USERS
  │
  ├──────────────► DONORS
  │                   │
  │                   └────────► DONATIONS
  │
  ├──────────────► EMERGENCY_REQUESTS
  │                       │
  │                       ├────────► HOSPITALS
  │                       │
  │                       └────────► MATCHES
  │                                      │
  │                                      └────────► MATCH_RESPONSES
  │
  └──────────────► NOTIFICATIONS