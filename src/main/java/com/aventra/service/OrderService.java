package com.aventra.service;

import com.aventra.dto.PlaceOrderRequest;
import com.aventra.dto.PlaceOrderResponse;
import com.aventra.dto.VerifyPaymentRequest;
import com.aventra.entity.Address;
import com.aventra.entity.CartItem;
import com.aventra.entity.CustomerOrder;
import com.aventra.entity.OrderItem;
import com.aventra.entity.Product;
import com.aventra.entity.User;
import com.aventra.repository.AddressRepository;
import com.aventra.repository.CartItemRepository;
import com.aventra.repository.CustomerOrderRepository;
import com.aventra.repository.OrderItemRepository;
import com.aventra.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OrderService {

    private static final Set<String> PAYMENT_METHODS = Set.of("COD", "UPI", "CARD");

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;
    private final PaymentService paymentService;
    private final EmailService emailService;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            OrderItemRepository orderItemRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            AddressRepository addressRepository,
            CurrentUserService currentUserService,
            PaymentService paymentService,
            EmailService emailService
    ) {
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.currentUserService = currentUserService;
        this.paymentService = paymentService;
        this.emailService = emailService;
    }

    /**
     * Creates the internal order before an online checkout opens. The order
     * stores the exact cart snapshot and reserves its stock, so the client
     * cannot manipulate the payment amount.
     */
    @Transactional
    public PlaceOrderResponse placeOrder(PlaceOrderRequest request) {
        User user = currentUserService.getCurrentUser();
        String paymentMethod = normalisePaymentMethod(request.getPaymentMethod());
        Address address = addressRepository.findByIdAndUser_Id(request.getAddressId(), user.getId())
                .orElseThrow(() -> new RuntimeException("Delivery address not found"));

        List<CartItem> cartItems = cartItemRepository.findByUser_Id(user.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        CustomerOrder order = new CustomerOrder();
        order.setUser(user);
        order.setDeliveryAddress(address);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PENDING");
        order.setStatus("COD".equals(paymentMethod) ? "CONFIRMED" : "PAYMENT_PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(0.0);
        order = customerOrderRepository.save(order);

        double totalAmount = reserveStockAndSaveItems(order, cartItems);
        order.setTotalAmount(totalAmount);
        order = customerOrderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);

        PlaceOrderResponse response = new PlaceOrderResponse(order);
        if ("COD".equals(paymentMethod)) {
            emailService.sendOrderConfirmationEmail(
                    user.getEmail(), order.getId(), totalAmount);
                    return response;
        }

        PaymentService.RazorpayCheckoutDetails checkout = paymentService.createRazorpayOrder(
                totalAmount,
                "aventra_" + order.getId()
        );
        order.setRazorpayOrderId(checkout.getRazorpayOrderId());
        customerOrderRepository.save(order);

        response.setRazorpayOrderId(checkout.getRazorpayOrderId());
        response.setRazorpayKeyId(checkout.getKeyId());
        response.setAmountInPaise(checkout.getAmountInPaise());
        response.setCurrency(checkout.getCurrency());
        return response;
    }

    @Transactional
    public CustomerOrder confirmOnlinePayment(VerifyPaymentRequest request) {
        User user = currentUserService.getCurrentUser();
        CustomerOrder order = getOwnedOrder(request.getOrderId(), user.getId());

        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            throw new RuntimeException("This order is not awaiting payment");
        }
        if (!request.getRazorpayOrderId().equals(order.getRazorpayOrderId())) {
            throw new RuntimeException("Payment order does not match the order");
        }

        paymentService.verifyPaymentSignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
               order.setPaymentStatus("PAID");
        order.setStatus("CONFIRMED");
        CustomerOrder savedOrder = customerOrderRepository.save(order);
        emailService.sendOrderConfirmationEmail(
                user.getEmail(), savedOrder.getId(), savedOrder.getTotalAmount());
        return savedOrder;
    }

    @Transactional
    public CustomerOrder cancelPendingOnlinePayment(Long orderId) {
        User user = currentUserService.getCurrentUser();
        CustomerOrder order = getOwnedOrder(orderId, user.getId());

        if (!"PAYMENT_PENDING".equals(order.getStatus())) {
            return order;
        }

        restoreStock(order);
        order.setPaymentStatus("FAILED");
        order.setStatus("CANCELLED");
        return customerOrderRepository.save(order);
    }

    public List<CustomerOrder> getUserOrders() {
        User user = currentUserService.getCurrentUser();
        return customerOrderRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        User user = currentUserService.getCurrentUser();
        getOwnedOrder(orderId, user.getId());
        return orderItemRepository.findByOrder_Id(orderId);
    }

    private double reserveStockAndSaveItems(CustomerOrder order, List<CartItem> cartItems) {
        double totalAmount = 0.0;

        for (CartItem cartItem : cartItems) {
            Product product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            int orderedQuantity = cartItem.getQuantity();
            int availableQuantity = product.getQuantity() == null ? 0 : product.getQuantity();

            if (availableQuantity < orderedQuantity) {
                throw new RuntimeException(product.getName() + " is out of stock");
            }

            product.setQuantity(availableQuantity - orderedQuantity);
            productRepository.save(product);

            double subTotal = product.getPrice() * orderedQuantity;
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(orderedQuantity);
            orderItem.setSubTotal(subTotal);
            orderItemRepository.save(orderItem);
            totalAmount += subTotal;
        }
        return totalAmount;
    }

    private void restoreStock(CustomerOrder order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_Id(order.getId());
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            int currentQuantity = product.getQuantity() == null ? 0 : product.getQuantity();
            product.setQuantity(currentQuantity + item.getQuantity());
            productRepository.save(product);
        }
    }

    private CustomerOrder getOwnedOrder(Long orderId, Long userId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("You cannot access another user's order");
        }
        return order;
    }

    private String normalisePaymentMethod(String requestedMethod) {
        String method = requestedMethod.trim().toUpperCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(method)) {
            throw new RuntimeException("Unsupported payment method");
        }
        return method;
    }
}
