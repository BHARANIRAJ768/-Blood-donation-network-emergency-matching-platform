package com.lifelink.util;

import com.lifelink.entity.BloodRequest;
import com.lifelink.entity.User;
import com.lifelink.enums.BloodGroup;

import java.util.Locale;

/**
 * Builds HTML bodies for transactional emails.
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static String donationRequestToDonor(User donor, BloodRequest request, Double distanceKm) {
        String distanceText = distanceKm == null || distanceKm.isInfinite()
                ? "Your district"
                : String.format(Locale.US, "%.1f km from you", distanceKm);
        String emergencyBadge = request.isEmergency()
                ? "<p style=\"color:#b91c1c;font-weight:700;font-size:15px;\">\u26A0 EMERGENCY REQUEST</p>"
                : "";

        return wrapper(
                "A blood request needs you",
                "<h2>Hello " + escape(donor.getName()) + ",</h2>"
                        + emergencyBadge
                        + "<p><strong>" + escape(request.getPatientName()) + "</strong> urgently needs "
                        + request.getUnits() + " unit(s) of <strong>" + request.getBloodGroup().getValue()
                        + "</strong> blood.</p>"
                        + "<ul>"
                        + "<li><strong>Hospital:</strong> " + escape(request.getHospitalName()) + "</li>"
                        + "<li><strong>Address:</strong> " + escape(orDash(request.getHospitalAddress())) + "</li>"
                        + "<li><strong>Reason:</strong> " + escape(orDash(request.getReason())) + "</li>"
                        + "<li><strong>Distance:</strong> " + distanceText + "</li>"
                        + "</ul>"
                        + "<p>If you can help, please log in to VitalDrop and accept the request from your "
                        + "notifications. Every minute counts.</p>"
                        + "<p style=\"color:#6b7280;font-size:13px;\">If you cannot donate, please decline the "
                        + "request in the app so other donors are alerted faster.</p>"
        );
    }

    public static String donorNotificationMail(User donor, BloodRequest request, User requester, Integer patientAge, String requiredDate, Double distanceKm, String acceptToken, String declineToken) {
        return donorNotificationMail(donor, request, requester, patientAge, requiredDate, distanceKm,
                acceptToken, declineToken, resolveBackendBaseUrl(), resolveFrontendBaseUrl());
    }

    /**
     * Builds the donor notification mail using explicit base URLs.
     * Production callers pass the deployed backend/frontend URLs so emailed
     * accept/decline links point at Vercel instead of localhost.
     */
    public static String donorNotificationMail(User donor, BloodRequest request, User requester, Integer patientAge, String requiredDate, Double distanceKm, String acceptToken, String declineToken, String backendBaseUrl, String frontendBaseUrl) {
        String urgency = request.isEmergency() ? "Emergency" : "Normal";
        String distanceText = distanceKm == null || distanceKm.isInfinite()
                ? "Your district"
                : String.format(Locale.US, "%.1f km from you", distanceKm);
        String baseUrl = normalizeBaseUrl(backendBaseUrl, "http://localhost:8080");
        String frontendFallback = normalizeBaseUrl(frontendBaseUrl, "http://localhost:3000") + "/request.html";
        String acceptUrl = acceptToken != null ? baseUrl + "/api/requests/email-accept?token=" + acceptToken : frontendFallback;
        String declineUrl = declineToken != null ? baseUrl + "/api/requests/email-decline?token=" + declineToken : frontendFallback;
        return wrapper(
                "VitalDrop Blood Request - " + request.getBloodGroup().getValue(),
                "<p>Dear " + escape(donor.getName()) + ",</p>"
                        + "<p>You have received a new blood donation request through VitalDrop.</p>"
                        + "<p style=\"text-align:center;font-weight:800;letter-spacing:1px;color:#b91c1c;\">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</p>"
                        + "<p style=\"text-align:center;font-weight:800;color:#b91c1c;\">🩸 BLOOD REQUEST DETAILS</p>"
                        + "<p style=\"text-align:center;font-weight:800;letter-spacing:1px;color:#b91c1c;\">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</p>"
                        + "<table width=\"100%\" cellpadding=\"6\" cellspacing=\"0\" style=\"font-size:14px;\">"
                        + "<tr><td style=\"font-weight:700;width:140px;\">Patient Name</td><td>: " + escape(request.getPatientName()) + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Patient Age</td><td>: " + orDash(patientAge != null ? String.valueOf(patientAge) : null) + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Blood Group</td><td>: " + escape(request.getBloodGroup().getValue()) + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Units Needed</td><td>: " + request.getUnits() + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Urgency</td><td>: " + escape(urgency) + "</td></tr>"
                        + "</table>"
                        + "<p style=\"font-weight:800;margin:18px 0 8px;\">🏥 Hospital Details</p>"
                        + "<table width=\"100%\" cellpadding=\"6\" cellspacing=\"0\" style=\"font-size:14px;\">"
                        + "<tr><td style=\"font-weight:700;width:140px;\">Hospital</td><td>: " + escape(request.getHospitalName()) + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Location</td><td>: " + escape(orDash(request.getHospitalAddress())) + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Required By</td><td>: " + escape(orDash(requiredDate)) + "</td></tr>"
                        + "</table>"
                        + "<p style=\"font-weight:800;margin:18px 0 8px;\">👤 Requester Details</p>"
                        + "<table width=\"100%\" cellpadding=\"6\" cellspacing=\"0\" style=\"font-size:14px;\">"
                        + "<tr><td style=\"font-weight:700;width:140px;\">Requester</td><td>: " + escape(requester != null ? requester.getName() : request.getPatientName()) + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Mobile</td><td>: " + escape(request.getPhone()) + "</td></tr>"
                        + "<tr><td style=\"font-weight:700;\">Email</td><td>: " + escape(requester != null ? requester.getEmail() : "-") + "</td></tr>"
                        + "</table>"
                        + "<p style=\"font-weight:700;margin:18px 0 6px;\">Additional Information:</p>"
                        + "<p style=\"background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:12px;font-size:14px;\">" + escape(orDash(request.getReason())) + "</p>"
                        + "<p style=\"font-size:12px;color:#6b7280;\">Distance: " + escape(distanceText) + "</p>"
                        + "<p style=\"text-align:center;font-weight:800;letter-spacing:1px;color:#b91c1c;margin:18px 0;\">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</p>"
                        + "<p style=\"text-align:center;\">"
                        + "<a href=\"" + acceptUrl + "\" style=\"background:#16a34a;color:#ffffff;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:700;margin-right:10px;\">✅ ACCEPT REQUEST</a>"
                        + "<a href=\"" + declineUrl + "\" style=\"background:#dc2626;color:#ffffff;padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:700;\">❌ DECLINE REQUEST</a>"
                        + "</p>"
                        + "<p style=\"color:#6b7280;font-size:13px;text-align:center;\">If you accept the request, please contact the requester using the provided contact details and coordinate the donation.</p>"
                        + "<p>Thank you for helping save a life. ❤️</p>"
                        + "<p>Regards,<br><strong>VitalDrop</strong><br>Every Drop is Vital</p>"
        );
    }

    public static String requestAcceptedToPatient(User donor, BloodRequest request) {
        int arrival = Haversine.estimatedArrivalMinutes(
                Haversine.distanceKm(request.getPatient().getLatitude(), request.getPatient().getLongitude(),
                        donor.getLatitude(), donor.getLongitude()));
        return wrapper(
                "Your blood request has been accepted",
                "<h2>Great news!</h2>"
                        + "<p>Your blood request for <strong>" + request.getUnits() + " unit(s) of "
                        + request.getBloodGroup().getValue() + "</strong> at "
                        + escape(request.getHospitalName()) + " has been accepted by a donor.</p>"
                        + "<h3>Donor details</h3>"
                        + "<ul>"
                        + "<li><strong>Name:</strong> " + escape(donor.getName()) + "</li>"
                        + "<li><strong>Phone:</strong> " + escape(donor.getPhone()) + "</li>"
                        + "<li><strong>Blood group:</strong> " + donor.getBloodGroup().getValue() + "</li>"
                        + "<li><strong>Estimated arrival:</strong> ~" + arrival + " minutes</li>"
                        + "</ul>"
                        + "<p>Please contact the donor to coordinate the donation.</p>"
        );
    }

    public static String donationCompleted(User donor, User patient, BloodRequest request) {
        return wrapper(
                "Donation completed",
                "<h2>Thank you for saving a life!</h2>"
                        + "<p>A <strong>" + request.getBloodGroup().getValue() + "</strong> blood donation of "
                        + request.getUnits() + " unit(s) has been marked complete.</p>"
                        + "<ul>"
                        + "<li><strong>Donor:</strong> " + escape(donor.getName()) + "</li>"
                        + "<li><strong>Patient:</strong> " + escape(patient.getName()) + "</li>"
                        + "<li><strong>Hospital:</strong> " + escape(orDash(request.getHospitalName())) + "</li>"
                        + "</ul>"
                        + "<p>The donation has been recorded permanently in your history.</p>"
        );
    }

    public static String passwordReset(User user, String resetLink) {
        return wrapper(
                "Reset your password",
                "<h2>Hello " + escape(user.getName()) + ",</h2>"
                        + "<p>We received a request to reset your password. Click the button below to choose a "
                        + "new one. This link is valid for 30 minutes.</p>"
                        + "<p style=\"text-align:center;\"><a href=\"" + resetLink + "\" style=\""
                        + "background:#dc2626;color:#ffffff;padding:12px 24px;border-radius:8px;"
                        + "text-decoration:none;font-weight:700;\">Reset password</a></p>"
                        + "<p style=\"color:#6b7280;font-size:13px;\">If you did not request this, you can safely "
                        + "ignore this email.</p>"
        );
    }

    public static String welcome(User user) {
        return wrapper(
                "Welcome to VitalDrop",
                "<h2>Welcome, " + escape(user.getName()) + "!</h2>"
                        + "<p>Your " + user.getRole().name().toLowerCase() + " account has been created on "
                        + "VitalDrop - Every Drop is Vital.</p>"
                        + "<p>Keep your availability up to date so we can match you with people in need.</p>"
        );
    }

    private static String wrapper(String title, String innerHtml) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"></head>"
                + "<body style=\"margin:0;padding:0;background:#fef2f2;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#fef2f2;padding:24px 12px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #fecaca;\">"
                + "<tr><td style=\"background:linear-gradient(135deg,#dc2626,#b91c1c);padding:22px 28px;\">"
                 + "<span style=\"color:#ffffff;font-size:22px;font-weight:800;\">\u2764 VitalDrop</span>"
                 + "<span style=\"color:#fecaca;font-size:13px;display:block;margin-top:2px;\">Every Drop is Vital</span>"
                + "</td></tr>"
                + "<tr><td style=\"padding:28px;color:#111827;\">"
                + innerHtml
                + "<p style=\"margin-top:28px;color:#6b7280;font-size:12px;\">This is an automated message from "
                + "VitalDrop. Please do not reply to this email.</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private static String resolveBackendBaseUrl() {
        String fromEnv = System.getenv("BACKEND_URL");
        return (fromEnv == null || fromEnv.isBlank()) ? "http://localhost:8080" : fromEnv.trim();
    }

    private static String resolveFrontendBaseUrl() {
        String fromEnv = System.getenv("FRONTEND_URL");
        return (fromEnv == null || fromEnv.isBlank()) ? "http://localhost:3000" : fromEnv.trim();
    }

    private static String normalizeBaseUrl(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
