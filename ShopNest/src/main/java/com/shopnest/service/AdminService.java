package com.shopnest.service;

import com.shopnest.model.Order;
import com.shopnest.repository.OrderRepository;
import com.shopnest.repository.ProductRepository;
import com.shopnest.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service class handling administrative data aggregation.
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Autowired
    public AdminService(UserRepository userRepository, ProductRepository productRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Total number of users registered in the system.
     */
    public long getUserCount() {
        return userRepository.count();
    }

    /**
     * Total number of products listed in the store.
     */
    public long getProductCount() {
        return productRepository.count();
    }

    /**
     * Total number of orders placed in the system.
     */
    public long getOrderCount() {
        return orderRepository.count();
    }

    /**
     * Calculates total revenue generated from completed or confirmed sales.
     * Excludes cancelled orders.
     */
    public BigDecimal getTotalRevenue() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .filter(order -> order.getStatus() != Order.OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
