package com.aventra.dto;

import com.aventra.entity.CustomerOrder;

public class PlaceOrderResponse {

    private CustomerOrder order;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private Long amountInPaise;
    private String currency;

    public PlaceOrderResponse() {
    }

    public PlaceOrderResponse(CustomerOrder order) {
        this.order = order;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public void setRazorpayKeyId(String razorpayKeyId) {
        this.razorpayKeyId = razorpayKeyId;
    }

    public Long getAmountInPaise() {
        return amountInPaise;
    }

    public void setAmountInPaise(Long amountInPaise) {
        this.amountInPaise = amountInPaise;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
