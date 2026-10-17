package com.aventra.repository;

import com.aventra.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}