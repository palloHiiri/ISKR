package com.fuzis.mailer.messaging;

import com.fuzis.mailer.service.EmailService;
import com.fuzis.mailer.transfer.messaging.EmailDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailMessageListener {

    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.rabbit_messaging_queue_email}")
    public void handleEmailMessage(EmailDTO emailDTO) {
        try {
            log.info("Received email message for: {}, type: {}",
                    emailDTO.getEmail(), emailDTO.getType());

            emailService.sendEmail(emailDTO);
            log.info("Successfully processed email for: {}", emailDTO.getEmail());
        } catch (Exception e) {
            log.error("Error processing email message for: {}", emailDTO.getEmail(), e);
        }
    }
}