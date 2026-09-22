const express = require('express');
const { body, validationResult } = require('express-validator');
const jwt = require('jsonwebtoken');
const config = require('../config');
const mailer = require('../service/mailer');
const { donorRequestTemplate } = require('../templates/donorRequest');

const router = express.Router();

function authMiddleware(req, res, next) {
  const auth = req.headers.authorization;
  if (!auth || !auth.startsWith('Bearer ')) return res.status(401).json({ message: 'Missing token' });
  const token = auth.substring(7);
  const secrets = [config.jwtSecret];
  try {
    // Spring's app.jwt.secret is base64-encoded, Node's jsonwebtoken expects raw or base64 decoded
    try { secrets.push(Buffer.from(config.jwtSecret, 'base64').toString('utf-8')); } catch(e) {}
    try { secrets.push(Buffer.from(config.jwtSecret, 'base64').toString('base64')); } catch(e) {}
  } catch(e) {}
  for (const sec of secrets) {
    try {
      const payload = jwt.verify(token, sec);
      req.user = payload;
      return next();
    } catch (e) {
      // try next secret
    }
  }
  return res.status(401).json({ message: 'Invalid token' });
}

// Health check without auth
router.get('/health', (req, res) => {
  res.json({ status: 'ok', smtp: !!(config.smtp.user && config.smtp.pass), from: config.mailFrom });
});

router.post('/send',
  [
    body('to').isEmail().withMessage('to must be valid email'),
    body('subject').notEmpty().withMessage('subject required'),
    body('html').notEmpty().withMessage('html required'),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });
    const { to, subject, html, text } = req.body;
    try {
      const info = await mailer.sendMail({ to, subject, html, text });
      res.json({ message: info.simulated ? 'Email simulated (SMTP not configured)' : 'Email sent', messageId: info.messageId });
    } catch (e) {
      console.error('mail send failed', e);
      res.status(500).json({ message: 'Failed to send email', error: e.message });
    }
  }
);

router.post('/donor-request',
  [
    body('to').isEmail(),
    body('donorName').notEmpty(),
    body('patientName').notEmpty(),
    body('bloodGroup').notEmpty(),
    body('hospitalName').notEmpty(),
    body('requesterEmail').isEmail(),
    body('requesterMobile').matches(/^[0-9]{10}$/),
    body('unitsNeeded').isInt({ min: 1, max: 10 }),
  ],
  async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });
    const html = donorRequestTemplate({
      donorName: req.body.donorName,
      patientName: req.body.patientName,
      patientAge: req.body.patientAge,
      bloodGroup: req.body.bloodGroup,
      unitsNeeded: req.body.unitsNeeded,
      urgency: req.body.urgency,
      hospitalName: req.body.hospitalName,
      hospitalLocation: req.body.hospitalLocation,
      requiredDate: req.body.requiredDate,
      requesterName: req.body.requesterName,
      requesterMobile: req.body.requesterMobile,
      requesterEmail: req.body.requesterEmail,
      additionalDetails: req.body.additionalDetails,
      acceptUrl: `${config.frontendUrl}/request.html`,
      declineUrl: `${config.frontendUrl}/request.html`,
    });
    const subject = `LifeLink Blood Request - ${req.body.bloodGroup} needed at ${req.body.hospitalName}`;
    try {
      const info = await mailer.sendMail({ to: req.body.to, subject, html });
      res.json({ message: info.simulated ? 'Email simulated' : 'Email sent', messageId: info.messageId });
    } catch (e) {
      console.error('donor mail failed', e);
      res.status(500).json({ message: 'Failed to send donor mail', error: e.message });
    }
  }
);

module.exports = router;
