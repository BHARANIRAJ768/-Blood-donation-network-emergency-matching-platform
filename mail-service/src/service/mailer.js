const nodemailer = require('nodemailer');
const config = require('../config');

let transporter = null;

function getTransporter() {
  if (transporter) return transporter;
  transporter = nodemailer.createTransport({
    host: config.smtp.host,
    port: config.smtp.port,
    secure: config.smtp.secure,
    auth: config.smtp.user && config.smtp.pass ? {
      user: config.smtp.user,
      pass: config.smtp.pass,
    } : undefined,
  });
  return transporter;
}

async function verify() {
  try {
    await getTransporter().verify();
    console.log('[mail-service] SMTP verified:', config.smtp.host);
    return true;
  } catch (e) {
    console.warn('[mail-service] SMTP verify failed:', e.message);
    return false;
  }
}

async function sendMail({ to, subject, html, text }) {
  if (!config.smtp.user || !config.smtp.pass) {
    console.log(`\n===== [EMAIL SIMULATED to ${to}] =====\nSUBJECT: ${subject}\n${html ? html.substring(0, 800) : text}\n===========================`);
    return { simulated: true, messageId: 'simulated-' + Date.now() };
  }
  const info = await getTransporter().sendMail({
    from: config.mailFrom,
    to,
    subject,
    html,
    text: text || html.replace(/<[^>]*>/g, ''),
  });
  console.log(`[mail-service] Email sent to ${to} messageId ${info.messageId}`);
  return info;
}

module.exports = { getTransporter, verify, sendMail };
