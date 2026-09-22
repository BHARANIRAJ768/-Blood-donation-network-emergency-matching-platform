package com.lifelink.service;

import com.lifelink.util.EmailTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends transactional emails. When {@code app.email.enabled} is false the
 * message is logged instead, so development works without an SMTP server.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final RestTemplate restTemplate;
    private final String mailServiceUrl;
    private final boolean mailServiceEnabled;
    private final String backendBaseUrl;
    private final String frontendBaseUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.enabled:false}") boolean enabled,
                        @Value("${app.email.from:VitalDrop}") String from,
                        @Value("${app.mail-service.url:http://localhost:3001}") String mailServiceUrl,
                        @Value("${app.mail-service.enabled:false}") boolean mailServiceEnabled,
                        @Value("${app.backend-url:http://localhost:8080}") String backendBaseUrl,
                        @Value("${app.frontend-url:http://localhost:3000}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.restTemplate = new RestTemplate();
        this.mailServiceUrl = mailServiceUrl;
        this.mailServiceEnabled = mailServiceEnabled;
        this.backendBaseUrl = backendBaseUrl;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendDonationRequestToDonor(String to, com.lifelink.entity.User donor,
                                           com.lifelink.entity.BloodRequest request, Double distanceKm) {
        // Try Nodemailer first when enabled (for broadcast, use same delegation as targeted)
        if (mailServiceEnabled) {
            try {
                String html = EmailTemplates.donationRequestToDonor(donor, request, distanceKm);
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("to", to);
                payload.put("subject", "VitalDrop - A blood request needs you");
                payload.put("html", html);
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(payload, headers);
                restTemplate.postForObject(mailServiceUrl + "/api/mail/send", entity, java.util.Map.class);
                log.info("Email delegated to Nodemailer mail-service for {}", to);
                return;
            } catch (Exception ex) {
                log.warn("Nodemailer mail-service failed for {}, fallback to JavaMail: {}", to, ex.getMessage());
            }
        }
        send(to, "VitalDrop - A blood request needs you",
                EmailTemplates.donationRequestToDonor(donor, request, distanceKm));
    }

    public void sendDonorNotificationMail(String to, com.lifelink.entity.User donor,
                                          com.lifelink.entity.BloodRequest request,
                                          com.lifelink.entity.User requester,
                                          Integer patientAge, String requiredDate, Double distanceKm) {
        sendDonorNotificationMail(to, donor, request, requester, patientAge, requiredDate, distanceKm, null, null);
    }

    public void sendDonorNotificationMail(String to, com.lifelink.entity.User donor,
                                          com.lifelink.entity.BloodRequest request,
                                          com.lifelink.entity.User requester,
                                          Integer patientAge, String requiredDate, Double distanceKm,
                                          String acceptToken, String declineToken) {
        // Try Nodemailer via mail-service first (as requested), fallback to JavaMail
        if (mailServiceEnabled) {
            try {
                String html = EmailTemplates.donorNotificationMail(donor, request, requester, patientAge, requiredDate, distanceKm, acceptToken, declineToken, backendBaseUrl, frontendBaseUrl);
                Map<String, Object> payload = new HashMap<>();
                payload.put("to", to);
                payload.put("donorName", donor.getName());
                payload.put("patientName", request.getPatientName());
                payload.put("patientAge", patientAge);
                payload.put("bloodGroup", request.getBloodGroup().getValue());
                payload.put("unitsNeeded", request.getUnits());
                payload.put("urgency", request.isEmergency() ? "Emergency" : "Normal");
                payload.put("hospitalName", request.getHospitalName());
                payload.put("hospitalLocation", request.getHospitalAddress());
                payload.put("requiredDate", requiredDate);
                payload.put("requesterName", requester != null ? requester.getName() : request.getPatientName());
                payload.put("requesterMobile", request.getPhone());
                payload.put("requesterEmail", requester != null ? requester.getEmail() : "");
                payload.put("additionalDetails", request.getReason());
                payload.put("subject", "VitalDrop - You have received a blood donation request");
                payload.put("html", html);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
                restTemplate.postForObject(mailServiceUrl + "/api/mail/send", entity, Map.class);
                log.info("Email delegated to Nodemailer mail-service for {}", to);
                return;
            } catch (Exception ex) {
                log.warn("Nodemailer mail-service failed for {}, fallback to JavaMail: {}", to, ex.getMessage());
            }
        }
        send(to, "VitalDrop - You have received a blood donation request",
                EmailTemplates.donorNotificationMail(donor, request, requester, patientAge, requiredDate, distanceKm, acceptToken, declineToken, backendBaseUrl, frontendBaseUrl));
    }

    public void sendRequestAcceptedToPatient(String to, com.lifelink.entity.User donor, com.lifelink.entity.BloodRequest request) {
        send(to, "VitalDrop - Your blood request has been accepted",
                EmailTemplates.requestAcceptedToPatient(donor, request));
    }

    public void sendDonationCompleted(String to, com.lifelink.entity.User donor, com.lifelink.entity.User patient,
                                      com.lifelink.entity.BloodRequest request) {
        send(to, "VitalDrop - Donation completed",
                EmailTemplates.donationCompleted(donor, patient, request));
    }

    public void sendPasswordReset(String to, com.lifelink.entity.User user, String resetLink) {
        send(to, "VitalDrop - Reset your password", EmailTemplates.passwordReset(user, resetLink));
    }

    public void sendWelcome(String to, com.lifelink.entity.User user) {
        send(to, "Welcome to VitalDrop", EmailTemplates.welcome(user));
    }

    private void send(String to, String subject, String htmlBody) {
        if (!enabled) {
            log.info("\n===== [EMAIL SIMULATED] =====\nTO: {}\nSUBJECT: {}\n{}\n============================", to, subject, htmlBody);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setFrom(from);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {} subject {}", to, subject);
        } catch (MailException | jakarta.mail.MessagingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage(), ex);
        }
    }
}
