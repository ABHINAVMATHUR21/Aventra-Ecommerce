package com.aventra.controller;

import com.aventra.dto.VerifyPaymentRequest;
import com.aventra.entity.CustomerOrder;
import com.aventra.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final OrderService orderService;

    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/verify")
    public CustomerOrder verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        return orderService.confirmOnlinePayment(request);
    }
}
