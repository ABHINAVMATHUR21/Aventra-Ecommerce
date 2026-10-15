package com.aventra.controller;

import com.aventra.dto.AdminDashboardResponse;
import com.aventra.dto.AdminOrderResponse;
import com.aventra.dto.UpdateOrderStatusRequest;
import com.aventra.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/orders")
    public List<AdminOrderResponse> getAllOrders() {
        return adminService.getAllOrders();
    }

    @PatchMapping("/orders/{orderId}/status")
    public AdminOrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return adminService.updateOrderStatus(orderId, request.getStatus());
    }
}
