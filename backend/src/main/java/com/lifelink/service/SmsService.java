package com.lifelink.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mobile SMS notification. When app.sms.enabled=false (default) the message is
 * logged instead, so development works without an external SMS provider.
 * When enabled, this is the single place to integrate a real provider (Twilio, etc.)
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final boolean enabled;
    private final String from;

    public SmsService(@Value("${app.sms.enabled:false}") boolean enabled,
                      @Value("${app.sms.from:VitalDrop}") String from) {
        this.enabled = enabled;
        this.from = from;
    }

    public void sendSms(String phone, String message) {
        if (phone == null || phone.isBlank()) {
            log.warn("SMS not sent: donor phone is missing");
            return;
        }
        if (!enabled) {
            log.info("\n===== [SMS SIMULATED to {}] =====\nFROM: {}\nMESSAGE: {}\n============================", phone, from, message);
            return;
        }
        // Real provider integration point (e.g., Twilio) - currently logs when enabled
        try {
            log.info("Sending SMS to {}: {}", phone, message);
            // TODO: integrate real SMS provider here using phone and message
        } catch (Exception ex) {
            log.error("Failed to send SMS to {}: {}", phone, ex.getMessage());
        }
    }
}
