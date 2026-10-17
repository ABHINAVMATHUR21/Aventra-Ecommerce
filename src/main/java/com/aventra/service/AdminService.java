package com.aventra.service;

import com.aventra.dto.AdminDashboardResponse;
import com.aventra.dto.AdminOrderResponse;
import com.aventra.entity.CustomerOrder;
import com.aventra.entity.OrderItem;
import com.aventra.entity.Product;
import com.aventra.repository.CustomerOrderRepository;
import com.aventra.repository.OrderItemRepository;
import com.aventra.repository.ProductRepository;
import com.aventra.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdminService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of(
            "CONFIRMED", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED"
    );

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminService(
            UserRepository userRepository,
            ProductRepository productRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public AdminDashboardResponse getDashboard() {
        List<CustomerOrder> orders = customerOrderRepository.findAllByOrderByCreatedAtDesc();
        List<Product> products = productRepository.findAll();

        double revenue = orders.stream()
                .filter(order -> !"CANCELLED".equalsIgnoreCase(order.getStatus()))
                .mapToDouble(order -> order.getTotalAmount() == null ? 0.0 : order.getTotalAmount())
                .sum();

        long ordersToProcess = orders.stream()
                .filter(order -> "CONFIRMED".equalsIgnoreCase(order.getStatus())
                        || "PACKED".equalsIgnoreCase(order.getStatus()))
                .count();

        long lowStockProducts = products.stream()
                .filter(product -> product.getQuantity() != null
                        && product.getQuantity() <= LOW_STOCK_THRESHOLD)
                .count();

        List<AdminOrderResponse> recentOrders = orders.stream()
                .limit(8)
                .map(this::toOrderResponse)
                .toList();

        return new AdminDashboardResponse(
                userRepository.count(),
                products.size(),
                orders.size(),
                revenue,
                ordersToProcess,
                lowStockProducts,
                recentOrders
        );
    }

    public List<AdminOrderResponse> getAllOrders() {
        return customerOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional
    public AdminOrderResponse updateOrderStatus(Long orderId, String requestedStatus) {
        String status = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ORDER_STATUSES.contains(status)) {
            throw new RuntimeException("Invalid order status");
        }

        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            if (!"CANCELLED".equals(status)) {
                throw new RuntimeException("A cancelled order cannot be reactivated");
            }
            return toOrderResponse(order);
        }

        // Stock is deducted when the order is placed. Return it exactly once
        // if an administrator cancels the order.
        if ("CANCELLED".equals(status)) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
            for (OrderItem item : orderItems) {
                Product product = item.getProduct();
                int currentStock = product.getQuantity() == null ? 0 : product.getQuantity();
                product.setQuantity(currentStock + item.getQuantity());
                productRepository.save(product);
            }
        }

        order.setStatus(status);
        return toOrderResponse(customerOrderRepository.save(order));
    }

    private AdminOrderResponse toOrderResponse(CustomerOrder order) {
        String customerName = order.getUser() == null ? "Customer" : order.getUser().getName();
        String customerEmail = order.getUser() == null ? "" : order.getUser().getEmail();
        String deliveryCity = order.getDeliveryAddress() == null ? "" : order.getDeliveryAddress().getCity();

        return new AdminOrderResponse(
                order.getId(),
                customerName,
                customerEmail,
                deliveryCity,
                order.getStatus(),
                order.getTotalAmount(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getCreatedAt()
        );
    }
}
