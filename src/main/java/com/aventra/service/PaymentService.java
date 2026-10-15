package com.aventra.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    public RazorpayCheckoutDetails createRazorpayOrder(
            double amountInRupees,
            String receipt
    ) {

        long amountInPaise = Math.round(amountInRupees * 100);

        if (!razorpayEnabled) {
            return new RazorpayCheckoutDetails(
                    "order_dev_" + UUID.randomUUID(),
                    razorpayKeyId.isBlank() ? "dev_key" : razorpayKeyId,
                    amountInPaise,
                    "INR"
            );
        }

        try {
            RazorpayClient client = new RazorpayClient(
                    razorpayKeyId,
                    razorpayKeySecret
            );

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);

            Order order = client.orders.create(orderRequest);

            return new RazorpayCheckoutDetails(
                    order.get("id"),
                    razorpayKeyId,
                    amountInPaise,
                    "INR"
            );

        } catch (RazorpayException exception) {
            throw new RuntimeException(
                    "Unable to create Razorpay order: "
                            + exception.getMessage()
            );
        }
    }

    public void verifyPaymentSignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {

        if (!razorpayEnabled) {
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("razorpay_order_id", razorpayOrderId);
            payload.put("razorpay_payment_id", razorpayPaymentId);
            payload.put("razorpay_signature", razorpaySignature);

            boolean valid = Utils.verifyPaymentSignature(
                    payload,
                    razorpayKeySecret
            );

            if (!valid) {
                throw new RuntimeException("Invalid payment signature");
            }

        } catch (RazorpayException exception) {
            throw new RuntimeException(
                    "Payment verification failed: "
                            + exception.getMessage()
            );
        }
    }

    public static class RazorpayCheckoutDetails {

        private final String razorpayOrderId;
        private final String keyId;
        private final long amountInPaise;
        private final String currency;

        public RazorpayCheckoutDetails(
                String razorpayOrderId,
                String keyId,
                long amountInPaise,
                String currency
        ) {
            this.razorpayOrderId = razorpayOrderId;
            this.keyId = keyId;
            this.amountInPaise = amountInPaise;
            this.currency = currency;
        }

        public String getRazorpayOrderId() {
            return razorpayOrderId;
        }

        public String getKeyId() {
            return keyId;
        }

        public long getAmountInPaise() {
            return amountInPaise;
        }

        public String getCurrency() {
            return currency;
        }
    }
}
