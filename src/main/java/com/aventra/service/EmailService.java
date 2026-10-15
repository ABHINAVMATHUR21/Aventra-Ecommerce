package com.aventra.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.json.JSONObject;
import com.razorpay.RazorpayClient;
@Service
public class EmailService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {

        if (!mailEnabled) {
            LOGGER.info("[DEV MODE] OTP for {}: {}", toEmail, otp);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Aventra OTP Verification");
        message.setText(
                "Your Aventra verification OTP is: "
                        + otp
                        + "\n\nThis OTP is valid for 10 minutes."
        );

        mailSender.send(message);
    }

    public void sendOrderConfirmationEmail(
            String toEmail,
            Long orderId,
            Double totalAmount
    ) {

        if (!mailEnabled) {
            LOGGER.info(
                    "[DEV MODE] Order confirmation for {}: order #{}, amount {}",
                    toEmail,
                    orderId,
                    totalAmount
            );
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Aventra Order Confirmed");
        message.setText(
                "Your order #"
                        + orderId
                        + " has been confirmed.\nTotal amount: Rs "
                        + totalAmount
                        + "\n\nThank you for shopping with Aventra!"
        );

        mailSender.send(message);
    }
}
