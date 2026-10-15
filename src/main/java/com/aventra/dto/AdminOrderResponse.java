package com.aventra.dto;

import java.time.LocalDateTime;

/** Safe order information shown only in the administrator dashboard. */
public record AdminOrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        String deliveryCity,
        String status,
        Double totalAmount,
        String paymentMethod,
        String paymentStatus,
        LocalDateTime createdAt
) {
}
