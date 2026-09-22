const express = require('express');
const cors = require('cors');
const config = require('./config');
const mailer = require('./service/mailer');

const app = express();
app.use(cors({
  origin: [config.frontendUrl, config.backendUrl, 'http://localhost:3000', 'http://localhost:8080'],
  credentials: true,
}));
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ status: 'ok', port: config.port, smtpConfigured: !!(config.smtp.user && config.smtp.pass) });
});
app.use('/api/mail', require('./routes/mail'));

app.use((req, res) => res.status(404).json({ message: 'Not found' }));

app.listen(config.port, async () => {
  console.log(`[mail-service] listening on http://localhost:${config.port}`);
  console.log(`[mail-service] SMTP ${config.smtp.host}:${config.smtp.port} user ${config.smtp.user || '(not configured - simulated)'}`);
  await mailer.verify();
});
