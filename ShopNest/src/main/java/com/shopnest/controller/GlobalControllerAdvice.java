package com.shopnest.controller;

import com.shopnest.model.Cart;
import com.shopnest.model.Category;
import com.shopnest.model.User;
import com.shopnest.service.CartService;
import com.shopnest.service.ProductService;
import com.shopnest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.List;

/**
 * Controller advice class providing global model attributes to all Thymeleaf templates.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;
    private final CartService cartService;
    private final ProductService productService;

    @Autowired
    public GlobalControllerAdvice(UserService userService, CartService cartService, ProductService productService) {
        this.userService = userService;
        this.cartService = cartService;
        this.productService = productService;
    }

    /**
     * Exposes current logged-in user details to all views.
     */
    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findByEmail(principal.getName()).orElse(null);
    }

    /**
     * Exposes current cart item count to all views.
     */
    @ModelAttribute("cartSize")
    public int cartSize(Principal principal) {
        if (principal == null) {
            return 0;
        }
        return userService.findByEmail(principal.getName())
                .map(user -> {
                    Cart cart = cartService.getCartByUser(user);
                    return cart.getCartItems().stream()
                            .mapToInt(item -> item.getQuantity())
                            .sum();
                })
                .orElse(0);
    }

    /**
     * Exposes all category models to all views.
     */
    @ModelAttribute("allCategories")
    public List<Category> allCategories() {
        return productService.getAllCategories();
    }
}
