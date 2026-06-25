package com.shopnest.controller;

import com.shopnest.model.Cart;
import com.shopnest.model.CartItem;
import com.shopnest.model.User;
import com.shopnest.service.CartService;
import com.shopnest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

/**
 * Controller handling user shopping cart operations: views, additions, quantity updates, and removals.
 */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @Autowired
    public CartController(CartService cartService, UserService userService) {
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
     * Renders the user's shopping cart page.
     * Computes the running total of all items in the cart.
     */
    @GetMapping
    public String viewCart(Model model, Principal principal) {
        User user = getAuthenticatedUser(principal);
        Cart cart = cartService.getCartByUser(user);
        
        BigDecimal total = cart.getCartItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", total);
        model.addAttribute("currentUser", user);
        return "cart/cart";
    }

    /**
     * Adds an item to the shopping cart.
     */
    @PostMapping("/add")
    public String addToCart(@RequestParam("productId") Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(principal);
            cartService.addItemToCart(user, productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Product added to cart successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    /**
     * Updates quantity of an item in the cart.
     */
    @PostMapping("/update")
    public String updateCartItem(@RequestParam("cartItemId") Long cartItemId,
                                 @RequestParam("quantity") Integer quantity,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(principal);
            cartService.updateCartItemQuantity(user, cartItemId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Cart updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }

    /**
     * Removes an item from the cart.
     */
    @PostMapping("/remove")
    public String removeCartItem(@RequestParam("cartItemId") Long cartItemId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = getAuthenticatedUser(principal);
            cartService.removeItemFromCart(user, cartItemId);
            redirectAttributes.addFlashAttribute("successMessage", "Item removed from cart.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/cart";
    }
}
