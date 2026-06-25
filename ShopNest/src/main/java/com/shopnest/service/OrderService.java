package com.shopnest.service;

import com.shopnest.model.*;
import com.shopnest.repository.OrderRepository;
import com.shopnest.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service class managing checkout procedures, order tracking, and status transitions.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    @Autowired
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, CartService cartService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    /**
     * Converts a user's active shopping cart into a placed order.
     * Decrements product stock levels and clears the user's cart.
     */
    public Order createOrder(User user, String address) {
        Cart cart = cartService.getCartByUser(user);
        if (cart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Cannot checkout with an empty cart");
        }

        // Initialize order
        Order order = Order.builder()
                .user(user)
                .address(address)
                .status(Order.OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            // Verify inventory
            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for item: " + product.getName());
            }

            // Deduct inventory stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            // Compute line item total and capture historical pricing snapshot
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        // Save order (cascades to orderItems)
        Order savedOrder = orderRepository.save(order);

        // Empty user shopping cart
        cartService.clearCart(cart);

        return savedOrder;
    }

    /**
     * Retrieves an order by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * Retrieves order history for a specific customer.
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Retrieves all orders for administrative purposes.
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Updates status of an existing order.
     */
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
