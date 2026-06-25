package com.shopnest.service;

import com.shopnest.model.Cart;
import com.shopnest.model.CartItem;
import com.shopnest.model.Product;
import com.shopnest.model.User;
import com.shopnest.repository.CartItemRepository;
import com.shopnest.repository.CartRepository;
import com.shopnest.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class handling cart actions: adding items, updating quantities, and clearing cart.
 */
@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    @Autowired
    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    /**
     * Retrieves the cart associated with a user.
     * Creates a new cart if one is not found.
     */
    public Cart getCartByUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Adds a product to the user's cart. If the product already exists, increments the quantity.
     */
    public void addItemToCart(User user, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Cart cart = getCartByUser(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        // Check if product stock is sufficient
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
        }

        // Check if item already exists in the cart
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            int newQuantity = item.getQuantity() + quantity;
            if (product.getStock() < newQuantity) {
                throw new IllegalArgumentException("Cannot add more. Insufficient stock for: " + product.getName());
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(newItem);
        }
    }

    /**
     * Updates the quantity of a cart item.
     */
    public void updateCartItemQuantity(User user, Long cartItemId, int quantity) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        // Secure operation: verify the cart belongs to the requesting user
        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized cart modification attempt");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(cartItem);
            return;
        }

        // Check stock availability
        if (cartItem.getProduct().getStock() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for product: " + cartItem.getProduct().getName());
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
    }

    /**
     * Removes an item from the cart.
     */
    public void removeItemFromCart(User user, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized cart modification attempt");
        }

        cartItemRepository.delete(cartItem);
    }

    /**
     * Clears all items in the user's cart.
     */
    public void clearCart(Cart cart) {
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }
}
