package com.aventra.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EmailService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmailService.class);

    private static final String BREVO_URL =
            "https://api.brevo.com/v3/smtp/email";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${BREVO_API_KEY:}")
    private String apiKey;

    @Value("${MAIL_FROM:}")
    private String fromEmail;

    @Value("${MAIL_FROM_NAME:Aventra}")
    private String fromName;

    public void sendOtpEmail(String toEmail, String otp) {

        if (!mailEnabled) {
            LOGGER.info("[DEV MODE] OTP for {}: {}", toEmail, otp);
            return;
        }

        send(
                toEmail,
                "Aventra OTP Verification",
                "Your Aventra verification OTP is: "
                        + otp
                        + "\n\nThis OTP is valid for 10 minutes."
        );
    }

    public void sendOrderConfirmationEmail(
            String toEmail,
            Long orderId,
            Double totalAmount
    ) {

        if (!mailEnabled) {
            LOGGER.info(
                    "[DEV MODE] Order confirmation for {}: order #{}, amount {}",
                    toEmail, orderId, totalAmount
            );
            return;
        }

        try {
            send(
                    toEmail,
                    "Aventra Order Confirmed",
                    "Your order #" + orderId
                            + " has been confirmed.\nTotal amount: Rs "
                            + totalAmount
                            + "\n\nThank you for shopping with Aventra!"
            );
        } catch (RuntimeException e) {
            LOGGER.error("Order confirmation email failed for order #{}", orderId, e);
        }
    }

    private void send(String to, String subject, String text) {

        if (apiKey.isBlank() || fromEmail.isBlank()) {
            throw new RuntimeException("Email service is not configured");
        }

        try {
            JSONObject body = new JSONObject();
            body.put("sender", new JSONObject()
                    .put("name", fromName)
                    .put("email", fromEmail));
            body.put("to", new JSONArray()
                    .put(new JSONObject().put("email", to)));
            body.put("subject", subject);
            body.put("textContent", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 300) {
                LOGGER.error("Brevo error {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("Could not send email. Please try again.");
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Email sending failed", e);
            throw new RuntimeException("Could not send email. Please try again.");
        }
    }
}
