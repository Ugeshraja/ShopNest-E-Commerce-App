package com.shopnest.config;

import com.shopnest.model.*;
import com.shopnest.repository.CategoryRepository;
import com.shopnest.repository.ProductRepository;
import com.shopnest.repository.UserRepository;
import com.shopnest.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Seeds default roles, administrative and user accounts, and product catalog data on application startup.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DatabaseSeeder(UserRepository userRepository,
                          CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          CartRepository cartRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Categories if empty
        if (categoryRepository.count() == 0) {
            Category electronics = Category.builder().name("Electronics").description("Latest gadgets, laptops, smartphones, and accessories.").build();
            Category fashion = Category.builder().name("Fashion").description("Trendy clothing, apparel, footwear, and accessories.").build();
            Category homeLiving = Category.builder().name("Home & Living").description("Furniture, home decor, kitchenware, and textiles.").build();
            Category fitness = Category.builder().name("Sports & Fitness").description("Exercise gear, fitness trackers, and outdoor equipment.").build();

            categoryRepository.saveAll(Arrays.asList(electronics, fashion, homeLiving, fitness));
        }

        // Fetch categories for product reference
        Category electronics = categoryRepository.findByName("Electronics").orElse(null);
        Category fashion = categoryRepository.findByName("Fashion").orElse(null);
        Category homeLiving = categoryRepository.findByName("Home & Living").orElse(null);
        Category fitness = categoryRepository.findByName("Sports & Fitness").orElse(null);

        // 2. Seed Products if empty
        if (productRepository.count() == 0 && electronics != null) {
            List<Product> products = Arrays.asList(
                Product.builder()
                        .name("Premium Wireless Headphones")
                        .description("Active noise-cancelling over-ear headphones with 30-hour battery life and high-fidelity sound output.")
                        .price(new BigDecimal("44000.00"))
                        .stock(50)
                        .imageUrl("https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=60")
                        .category(electronics)
                        .build(),
                Product.builder()
                        .name("Smart Fitness Watch")
                        .description("Heart rate monitor, GPS tracking, sleep analyzer, and water-resistant smartwatch with 7-day battery life.")
                        .price(new BigDecimal("12500.00"))
                        .stock(35)
                        .imageUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=60")
                        .category(electronics)
                        .build(),
                Product.builder()
                        .name("Classic Leather Jacket")
                        .description("Handcrafted genuine leather jacket. Windproof, stylish, and perfect for all seasons.")
                        .price(new BigDecimal("9999.00"))
                        .stock(15)
                        .imageUrl("https://images.unsplash.com/photo-1551028719-00167b16eac5?w=500&auto=format&fit=crop&q=60")
                        .category(fashion)
                        .build(),
                Product.builder()
                        .name("Canvas Casual Backpack")
                        .description("Durable water-resistant canvas backpack with dedicated laptop sleeve, spacious chambers, and ergonomic straps.")
                        .price(new BigDecimal("2499.00"))
                        .stock(100)
                        .imageUrl("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500&auto=format&fit=crop&q=60")
                        .category(fashion)
                        .build(),
                Product.builder()
                        .name("Ceramic Minimalist Vase")
                        .description("Modern nordic-style white ceramic vase, ideal for displaying dry flowers or as a tabletop highlight.")
                        .price(new BigDecimal("1299.00"))
                        .stock(20)
                        .imageUrl("https://images.unsplash.com/photo-1578500494198-246f612d3b3d?w=500&auto=format&fit=crop&q=60")
                        .category(homeLiving)
                        .build(),
                Product.builder()
                        .name("Ergonomic Mesh Office Chair")
                        .description("High-back desk chair featuring adjustable lumbar support, 3D armrests, and dynamic tilt lock.")
                        .price(new BigDecimal("14999.00"))
                        .stock(10)
                        .imageUrl("https://images.unsplash.com/photo-1580481072645-022f9a6dbf27?w=500&auto=format&fit=crop&q=60")
                        .category(homeLiving)
                        .build(),
                Product.builder()
                        .name("Adjustable Dumbbells Set (20kg)")
                        .description("All-in-one dumbbell/barbell set with chrome plating and solid grip. Includes weight plates and spinlock collars.")
                        .price(new BigDecimal("7499.00"))
                        .stock(25)
                        .imageUrl("https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500&auto=format&fit=crop&q=60")
                        .category(fitness)
                        .build()
            );
            productRepository.saveAll(products);
        }

        // 3. Seed Users if empty
        if (userRepository.count() == 0) {
            // Seed Admin User
            User admin = User.builder()
                    .name("ShopNest Admin")
                    .email("admin@shopnest.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(User.Role.ADMIN)
                    .build();
            User savedAdmin = userRepository.save(admin);

            // Auto-create cart for Admin
            cartRepository.save(Cart.builder().user(savedAdmin).build());

            // Seed Regular User
            User user = User.builder()
                    .name("User Name")
                    .email("user@shopnest.com")
                    .password(passwordEncoder.encode("user123"))
                    .role(User.Role.USER)
                    .build();
            User savedUser = userRepository.save(user);

            // Auto-create cart for User
            cartRepository.save(Cart.builder().user(savedUser).build());
        }
    }
}
