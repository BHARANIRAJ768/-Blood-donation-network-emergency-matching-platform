# 🩸 Blood Donation Network & Emergency  Platform

## 1. Title
**Blood Donation Network & Emergency Matching Platform**

## 2. Domain
**Healthcare Technology / Blood Donation / Emergency Response**

## 3. Users

- **Donor:** Manages blood group, location, availability, and responds to requests.
- **Requester:** Creates emergency blood requests and views matched donors.
- **Admin:** Manages users, hospitals, requests, and platform activities.

## 4. Problem Statement

Finding compatible blood donors during emergencies can be difficult and time-consuming. Traditional methods may not quickly identify available nearby donors. This platform connects emergency blood requests with suitable donors based on blood group, availability, and location.

## 5. Proposed Solution

The application will provide:

- User registration and login
- Donor profile management
- Donor availability management
- Emergency blood requests
- Hospital management
- Blood donor matching
- Donor accept/reject response
- Notifications
- Admin dashboard

## 6. Core Entities / Database Tables

- Users
- Donors
- Donations
- Emergency Requests
- Hospitals
- Matches
- Match Responses
- Notifications
- Admin

## 7. User Roles & Permissions

| Role | Permissions |
|---|---|
| **Donor** | Manage profile, availability, view matches, accept/reject requests |
| **Requester** | Create requests, view matched donors, track requests |
| **Admin** | Manage users, hospitals, requests, donors, and matches |

## 8. Success Criteria

- User can register and log in.
- Requester can create an emergency blood request.
- System identifies suitable available donors.
- Donor can accept or reject a request.
- Notifications are sent for matches.
- Admin can monitor the platform.

## 9. Out of Scope

- Blood collection and storage
- Blood transportation
- Medical diagnosis
- Payment processing
- Real-time GPS tracking
- Government/hospital system integration
- Medical record verification

## 10. Chosen Track

**Java — Spring Boot**

### Technology Stack

- **Frontend:** React.js
- **Backend:** Java Spring Boot
- **Database:** MySQL
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security
- **API Testing:** Postman
- **Version Control:** Git & GitHub

## Current Status

- ✅ Project idea finalized
- ✅ Main workflow planned
- ✅ ER diagram designed
- 🟡 Database implementation pending
- 🟡 Spring Boot backend pending
- 🟡 Frontend pending
- 🟡 Matching engine pending
- 🟡 Authentication pending
- 🟡 Notifications pending
- 🟡 Deployment pending

## Development Flow

```text
Setup
 ↓
Database
 ↓
Spring Boot Backend
 ↓
Authentication
 ↓
Donor Module
 ↓
Emergency Request
 ↓
Matching Engine
 ↓
Notifications
 ↓
Admin Dashboard
 ↓
React Frontend
 ↓
Testing & Deployment
```
