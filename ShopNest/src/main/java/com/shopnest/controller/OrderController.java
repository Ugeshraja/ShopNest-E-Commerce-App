package com.shopnest.controller;

import com.shopnest.dto.CheckoutDTO;
import com.shopnest.model.Cart;
import com.shopnest.model.Order;
import com.shopnest.model.User;
import com.shopnest.service.CartService;
import com.shopnest.service.OrderService;
import com.shopnest.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * Controller handling customer checkout forms, order submission, receipt display, and history list.
 */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;

    @Autowired
    public OrderController(OrderService orderService, CartService cartService, UserService userService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new SecurityException("User is not authenticated");
        }
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
    }

    /**
     * Renders checkout form containing the user's active shopping cart items and billing summary.
     */
    @GetMapping("/checkout")
    public String showCheckoutPage(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal);
        Cart cart = cartService.getCartByUser(user);

        if (cart.getCartItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty. Add products before checkout.");
            return "redirect:/cart";
        }

        BigDecimal total = cart.getCartItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", total);
        model.addAttribute("checkoutDTO", new CheckoutDTO());
        model.addAttribute("currentUser", user);

        return "order/checkout";
    }

    /**
     * Processes checkout submission, mapping cart details to a confirmed Order database record.
     */
    @PostMapping("/checkout")
    public String processCheckout(@Valid @ModelAttribute("checkoutDTO") CheckoutDTO checkoutDTO,
                                  BindingResult bindingResult,
                                  Model model,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal);
        Cart cart = cartService.getCartByUser(user);

        if (bindingResult.hasErrors()) {
            BigDecimal total = cart.getCartItems().stream()
                    .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("cart", cart);
            model.addAttribute("cartTotal", total);
            model.addAttribute("currentUser", user);
            return "order/checkout";
        }

        try {
            Order order = orderService.createOrder(user, checkoutDTO.getAddress());
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully!");
            return "redirect:/orders/confirmation/" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Renders order placement receipt details after successful checkout.
     */
    @GetMapping("/confirmation/{id}")
    public String orderConfirmation(@PathVariable("id") Long id, Model model, Principal principal) {
        User user = getAuthenticatedUser(principal);
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + id));

        // Security check: restrict visibility to order owner or administrator
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Access Denied: Unauthorized order receipt viewing");
        }

        model.addAttribute("order", order);
        model.addAttribute("currentUser", user);
        return "order/confirmation";
    }

    /**
     * Renders complete purchase history of the logged-in customer.
     */
    @GetMapping("/history")
    public String orderHistory(Model model, Principal principal) {
        User user = getAuthenticatedUser(principal);
        List<Order> orders = orderService.getOrdersByUser(user);
        model.addAttribute("orders", orders);
        model.addAttribute("currentUser", user);
        return "order/history";
    }
}
