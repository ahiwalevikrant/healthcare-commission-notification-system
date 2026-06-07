package com.healthcare.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendPayoutNotification(String toEmail,
                                       String agentNpn,
                                       String carrierId,
                                       String month,
                                       BigDecimal amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("vikrantahiwale71@gmail.com");
            message.setTo(toEmail != null ? toEmail : "agent-" + agentNpn + "@gmail.com");
            message.setSubject("Commission Payout Notification - " + month);
            message.setText(buildEmailBody(agentNpn, carrierId, month, amount));

            mailSender.send(message);
            log.info("Email sent to agent NPN: {} for month: {}", agentNpn, month);

        } catch (Exception e) {
            log.error("Failed to send email for agent NPN: {}", agentNpn, e);
            throw e;
        }
    }

    private String buildEmailBody(String agentNpn,
                                  String carrierId,
                                  String month,
                                  BigDecimal amount) {
        return """
                Dear Agent,
                
                Your commission payout has been calculated for the following:
                
                Agent NPN   : %s
                Carrier     : %s
                Month       : %s
                Total Payout: $%s
                
                Please log in to the portal to view the details.
                
                Regards,
                Healthcare Commission System
                """.formatted(agentNpn, carrierId, month, amount);
    }

}
