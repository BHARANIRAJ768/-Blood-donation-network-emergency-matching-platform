require('dotenv').config();

function required(name) {
  const v = process.env[name];
  if (!v) console.warn(`[mail-service] WARN: ${name} not set`);
  return v;
}

module.exports = {
  port: parseInt(process.env.PORT || '3001', 10),
  smtp: {
    host: process.env.SMTP_HOST || 'smtp.gmail.com',
    port: parseInt(process.env.SMTP_PORT || '587', 10),
    secure: process.env.SMTP_SECURE === 'true',
    user: required('SMTP_USER'),
    pass: required('SMTP_PASS'),
  },
  mailFrom: process.env.MAIL_FROM || 'LifeLink <aravindmanickam2007@gmail.com>',
  frontendUrl: process.env.FRONTEND_URL || 'http://localhost:3000',
  backendUrl: process.env.BACKEND_URL || 'http://localhost:8080',
  jwtSecret: process.env.APP_JWT_SECRET,
};
