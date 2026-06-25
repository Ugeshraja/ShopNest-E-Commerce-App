package com.shopnest.controller;

import com.shopnest.model.Category;
import com.shopnest.model.Product;
import com.shopnest.model.User;
import com.shopnest.service.ProductService;
import com.shopnest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller handling product listings, filters, search, and detail views.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    @Autowired
    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @ModelAttribute("allCategories")
    public List<Category> allCategories() {
        return productService.getAllCategories();
    }

    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findByEmail(principal.getName()).orElse(null);
    }

    /**
     * Lists products, supporting search query filtering and category ID constraints.
     */
    @GetMapping
    public String listProducts(@RequestParam(value = "category", required = false) Long categoryId,
                               @RequestParam(value = "search", required = false) String search,
                               Model model) {
        List<Product> products = productService.searchProductsByCategory(categoryId, search);
        model.addAttribute("products", products);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("searchQuery", search);

        // Fetch selected category name for display
        if (categoryId != null && categoryId > 0) {
            productService.getCategoryById(categoryId).ifPresent(c -> model.addAttribute("selectedCategoryName", c.getName()));
        } else {
            model.addAttribute("selectedCategoryName", "All Products");
        }

        return "products/list";
    }

    /**
     * Displays details for a specific product.
     */
    @GetMapping("/{id}")
    public String productDetail(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
        model.addAttribute("product", product);
        return "products/detail";
    }
}
