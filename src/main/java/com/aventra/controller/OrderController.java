package com.aventra.controller;

import com.aventra.dto.PlaceOrderRequest;
import com.aventra.dto.PlaceOrderResponse;
import com.aventra.entity.CustomerOrder;
import com.aventra.entity.OrderItem;
import jakarta.validation.Valid;
import com.aventra.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public PlaceOrderResponse placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @GetMapping("/my-orders")
    public List<CustomerOrder> getUserOrders() {
        return orderService.getUserOrders();
    }

    @GetMapping("/{orderId}/items")
    public List<OrderItem> getOrderItems(
            @PathVariable Long orderId
    ) {
        return orderService.getOrderItems(orderId);
    }

    @PostMapping("/{orderId}/cancel-payment")
    public CustomerOrder cancelPendingPayment(@PathVariable Long orderId) {
        return orderService.cancelPendingOnlinePayment(orderId);
    }
}
