package com.shopnest.controller;

import com.shopnest.model.Category;
import com.shopnest.model.Product;
import com.shopnest.model.User;
import com.shopnest.service.ProductService;
import com.shopnest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller handling landing and home page view rendering.
 */
@Controller
public class HomeController {

    private final ProductService productService;
    private final UserService userService;

    @Autowired
    public HomeController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    /**
     * Exposes all categories globally to the view layer for nav bar inclusion.
     */
    @ModelAttribute("allCategories")
    public List<Category> allCategories() {
        return productService.getAllCategories();
    }

    /**
     * Exposes current logged-in user details to templates for layout adjustments.
     */
    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findByEmail(principal.getName()).orElse(null);
    }

    /**
     * Directs to the customized premium homepage.
     * Selects up to 6 products to feature on the homepage.
     */
    @GetMapping("/")
    public String index(Model model) {
        List<Product> products = productService.getAllProducts();
        // Take up to 6 products as featured
        List<Product> featuredProducts = products.stream()
                .limit(6)
                .collect(Collectors.toList());
        model.addAttribute("featuredProducts", featuredProducts);
        return "home";
    }

    /**
     * Optional mapping for explicit /home path.
     */
    @GetMapping("/home")
    public String home() {
        return "redirect:/";
    }
}
