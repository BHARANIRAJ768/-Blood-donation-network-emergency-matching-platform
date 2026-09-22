function esc(s) {
  if (s == null) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}
function orDash(v) { return v == null || String(v).trim() === '' ? '-' : String(v); }

function donorRequestTemplate(data) {
  const donorName = esc(data.donorName || 'Donor');
  const patientName = esc(data.patientName || '-');
  const patientAge = data.patientAge != null ? esc(String(data.patientAge)) : '-';
  const bloodGroup = esc(data.bloodGroup || '-');
  const unitsNeeded = esc(String(data.unitsNeeded ?? data.units ?? '-'));
  const urgency = esc(data.urgency || (data.emergency ? 'Emergency' : 'Normal'));
  const hospitalName = esc(data.hospitalName || '-');
  const hospitalLocation = esc(data.hospitalLocation || data.hospitalAddress || '-');
  const requiredDate = data.requiredDate ? esc(data.requiredDate) : '-';
  const requesterName = esc(data.requesterName || data.patientName || '-');
  const requesterMobile = esc(data.requesterMobile || data.phone || '-');
  const requesterEmail = esc(data.requesterEmail || '-');
  const additionalDetails = esc(data.additionalDetails || data.reason || '-');

  const acceptUrl = data.acceptUrl || '#';
  const declineUrl = data.declineUrl || '#';

  return `<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"></head><body style="margin:0;padding:0;background:#fef2f2;font-family:Arial,Helvetica,sans-serif;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#fef2f2;padding:24px 12px;">
<tr><td align="center">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:600px;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #fecaca;">
<tr><td style="background:linear-gradient(135deg,#dc2626,#b91c1c);padding:22px 28px;">
<span style="color:#ffffff;font-size:22px;font-weight:800;">&#10084; LifeLink</span>
<span style="color:#fecaca;font-size:13px;display:block;margin-top:2px;">Smart Blood Donation System</span>
</td></tr>
<tr><td style="padding:28px;color:#111827;">
<p>Dear ${donorName},</p>
<p>You have received a new blood donation request through LifeLink.</p>

<p style="text-align:center;font-weight:800;letter-spacing:1px;color:#b91c1c;margin:16px 0 8px;">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</p>
<p style="text-align:center;font-weight:800;color:#b91c1c;margin:0 0 8px;">🩸 BLOOD REQUEST DETAILS</p>
<p style="text-align:center;font-weight:800;letter-spacing:1px;color:#b91c1c;margin:0 0 16px;">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</p>

<table width="100%" cellpadding="6" cellspacing="0" style="font-size:14px;">
<tr><td style="font-weight:700;width:140px;">Patient Name</td><td>: ${patientName}</td></tr>
<tr><td style="font-weight:700;">Patient Age</td><td>: ${patientAge}</td></tr>
<tr><td style="font-weight:700;">Blood Group</td><td>: ${bloodGroup}</td></tr>
<tr><td style="font-weight:700;">Units Needed</td><td>: ${unitsNeeded}</td></tr>
<tr><td style="font-weight:700;">Urgency</td><td>: ${urgency}</td></tr>
</table>

<p style="font-weight:800;margin:18px 0 8px;">🏥 Hospital Details</p>
<table width="100%" cellpadding="6" cellspacing="0" style="font-size:14px;">
<tr><td style="font-weight:700;width:140px;">Hospital</td><td>: ${hospitalName}</td></tr>
<tr><td style="font-weight:700;">Location</td><td>: ${hospitalLocation}</td></tr>
<tr><td style="font-weight:700;">Required By</td><td>: ${requiredDate}</td></tr>
</table>

<p style="font-weight:800;margin:18px 0 8px;">👤 Requester Details</p>
<table width="100%" cellpadding="6" cellspacing="0" style="font-size:14px;">
<tr><td style="font-weight:700;width:140px;">Requester</td><td>: ${requesterName}</td></tr>
<tr><td style="font-weight:700;">Mobile</td><td>: ${requesterMobile}</td></tr>
<tr><td style="font-weight:700;">Email</td><td>: ${requesterEmail}</td></tr>
</table>

<p style="font-weight:700;margin:18px 0 6px;">Additional Information:</p>
<p style="background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:12px;font-size:14px;">${additionalDetails}</p>

<p style="text-align:center;font-weight:800;letter-spacing:1px;color:#b91c1c;margin:18px 0;">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</p>
<p style="text-align:center;font-weight:700;">Please respond to this request:</p>
<p style="text-align:center;margin:16px 0;">
<a href="${acceptUrl}" style="background:#16a34a;color:#ffffff;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:700;margin-right:10px;">✅ ACCEPT REQUEST</a>
<a href="${declineUrl}" style="background:#dc2626;color:#ffffff;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:700;">❌ DECLINE REQUEST</a>
</p>
<p style="color:#6b7280;font-size:13px;text-align:center;">If you accept the request, please contact the requester using the provided contact details and coordinate the donation.</p>
<p>Thank you for helping save a life. ❤️</p>
<p>Regards,<br><strong>LifeLink</strong><br>Smart Blood Donation System</p>
<p style="margin-top:28px;color:#6b7280;font-size:12px;">This is an automated message from LifeLink. Please do not reply to this email.</p>
</td></tr></table></td></tr></table></body></html>`;
}

module.exports = { donorRequestTemplate, esc, orDash };
