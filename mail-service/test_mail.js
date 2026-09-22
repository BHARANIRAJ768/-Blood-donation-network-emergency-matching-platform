const nodemailer = require('nodemailer');
async function test() {
  const transporter = nodemailer.createTransport({
    host: 'smtp.gmail.com', port: 587, secure: false,
    auth: { user: 'aravindmanickam2007@gmail.com', pass: 'nwokjagdonhnpwui' }
  });
  try {
    await transporter.verify();
    console.log('SMTP verify ok');
    const info = await transporter.sendMail({
      from: 'LifeLink <aravindmanickam2007@gmail.com>',
      to: 'aravindmanickam2007@gmail.com',
      subject: 'LifeLink Test',
      html: '<p>Test from LifeLink</p>'
    });
    console.log('sent', info.messageId);
  } catch(e) { console.error('fail', e.message) }
}
test();
