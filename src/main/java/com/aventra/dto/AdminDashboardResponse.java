package com.aventra.dto;

import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalProducts,
        long totalOrders,
        double totalRevenue,
        long ordersToProcess,
        long lowStockProducts,
        List<AdminOrderResponse> recentOrders
) {
}
