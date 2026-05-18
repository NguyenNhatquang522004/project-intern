package com.example.demo.common.share.email;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import com.example.demo.common.Dto.EmailRequest;
import com.example.demo.common.exception.EmailException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailAdapter implements IEmail {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async // Best Practice: Gửi email bất đồng bộ để không chặn thread chính
    @Override
    public void sendEmail(EmailRequest request) {
        try {
            log.info("Starting to send email to: {}", request.getTo());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            // Xử lý Template bằng Thymeleaf
            Context context = new Context();
            context.setVariables(request.getVariables());
            String htmlContent = templateEngine.process(request.getTemplateName(), context);

            // Thiết lập thông tin email
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            helper.setText(htmlContent, true);
            helper.setFrom(request.getTo());

            // 3. Xử lý đính kèm (nếu có)
            if (request.getAttachments() != null) {
                for (File file : request.getAttachments()) {
                    helper.addAttachment(file.getName(), file);
                }
            }

            mailSender.send(message);
            log.info("Email sent successfully to {}", request.getTo());

        } catch (MessagingException e) {
            log.error("Failed to send email to {}", request.getTo(), e);
            throw new EmailException("Error while sending email", e);
        }
    }
}
