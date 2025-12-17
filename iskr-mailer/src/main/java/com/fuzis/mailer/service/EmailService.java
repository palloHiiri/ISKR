package com.fuzis.mailer.service;

import com.fuzis.mailer.transfer.messaging.EmailDTO;
import com.fuzis.mailer.transfer.messaging.EmailType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
public class EmailService {

    @Value("${spring.mail.from}")
    private String emailFrom;

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;

    public void sendEmail(EmailDTO emailDTO) {
        try {
            String templateName = getTemplateName(emailDTO.getType());
            Map<String, Object> variables = extractVariables(emailDTO);

            String subject = getSubject(emailDTO.getType());
            String htmlContent = generateHtmlContent(templateName, variables);

            sendHtmlMessage(emailDTO.getEmail(), subject, htmlContent);

            log.info("Email sent successfully to: {}", emailDTO.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to: {}", emailDTO.getEmail(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String getTemplateName(EmailType type) {
        return switch (type) {
            case VerifyEmailEmail -> "verify-email";
            case ResetPasswordTokenEmail -> "reset-password";
            default -> throw new IllegalArgumentException("Unsupported email type: " + type);
        };
    }

    private String getSubject(EmailType type) {
        return switch (type) {
            case VerifyEmailEmail -> "Подтвердите свой email";
            case ResetPasswordTokenEmail -> "Сброс пароля";
            default -> "Уведомление";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractVariables(EmailDTO emailDTO) {
        if (emailDTO.getContent() instanceof Map) {
            return (Map<String, Object>) emailDTO.getContent();
        }
        if (emailDTO.getContent() instanceof String) {
            return Map.of("value", emailDTO.getContent());
        }
        throw new IllegalArgumentException("Unknown type of content: " + emailDTO.getContent());
    }

    private String generateHtmlContent(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return templateEngine.process(templateName, context);
    }

    private void sendHtmlMessage(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        log.warn("Sending email to: {}", to);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(emailFrom);
        mailSender.send(message);
    }
}