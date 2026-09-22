# LifeLink Mail Service (Nodemailer)

Node microservice for donor blood request notifications using **Nodemailer** + Gmail App Password.

- Port: **3001** (frontend 3000, backend 8080)
- SMTP: `smtp.gmail.com:587` via `aravindmanickam2007@gmail.com` / App Password `nwokjagdonhnpwui`

## Run
```bash
cd mail-service
npm install
cp .env.example .env   # already filled with your Gmail + App Password
npm run dev   # nodemon :3001
# or
npm start
```

Health: `GET http://localhost:3001/health` → `{status:"ok", smtpConfigured:true}`

## Endpoints (Bearer JWT required, same `APP_JWT_SECRET` as Spring Boot)
- `POST /api/mail/send` { to, subject, html, text? }
- `POST /api/mail/donor-request` { to, donorName, patientName, patientAge, bloodGroup, unitsNeeded, urgency, hospitalName, hospitalLocation, requiredDate, requesterName, requesterMobile, requesterEmail, additionalDetails }

Template renders the exact donor mail you provided with `Dear {{donorName}} ... LifeLink Smart Blood Donation System`.

## Integration
Spring Boot `EmailService` can delegate via `RestTemplate` to `http://localhost:3001/api/mail/donor-request` when `app.mail-service.enabled=true` (fallback to JavaMail when Node down). Frontend never sees credentials.

Credentials are in `mail-service/.env` (gitignored), never committed.
