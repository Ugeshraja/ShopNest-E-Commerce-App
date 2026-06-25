package com.shopnest.controller;
import com.shopnest.model.Category;
import com.shopnest.model.Order;
import com.shopnest.model.Product;
import com.shopnest.model.User;
import com.shopnest.service.AdminService;
import com.shopnest.service.ProductService;
import com.shopnest.service.OrderService;
import com.shopnest.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
/**
 * Controller handling restricted administrative operations: dashboard metrics, catalog adjustments, and order state updates.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;
    @Autowired
    public AdminController(AdminService adminService,
                           ProductService productService,
                           OrderService orderService,
                           UserService userService) {
        this.adminService = adminService;
        this.productService = productService;
        this.orderService = orderService;
        this.userService = userService;
    }
    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new SecurityException("User is not authenticated");
        }
        return userService.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user not found"));
    }
    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) return null;
        return userService.findByEmail(principal.getName()).orElse(null);
    }
    @ModelAttribute("activeTab")
    public String activeTab(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin/products")) {
            return "products";
        } else if (uri.startsWith("/admin/orders")) {
            return "orders";
        } else {
            return "dashboard";
        }
    }
    /**
     * Renders admin control dashboard with summary reports and statistical cards.
     */
    @GetMapping
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("userCount", adminService.getUserCount());
        model.addAttribute("productCount", adminService.getProductCount());
        model.addAttribute("orderCount", adminService.getOrderCount());
        model.addAttribute("totalRevenue", adminService.getTotalRevenue());
        return "admin/dashboard";
    }
    @GetMapping("/dashboard")
    public String dashboardRedirect() {
        return "redirect:/admin";
    }
    // --- Product Management ---
    /**
     * Lists all products for management table layout.
     */
    @GetMapping("/products")
    public String listProducts(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "admin/products";
    }
    /**
     * Renders form to add a new product.
     */
    @GetMapping("/products/add")
    public String showAddProductForm(Model model) {
        List<Category> categories = productService.getAllCategories();
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categories);
        model.addAttribute("isEdit", false);
        return "admin/product-form"; // We'll create a single reusable form for Add/Edit
    }
    /**
     * Renders form to edit an existing product.
     */
    @GetMapping("/products/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
            List<Category> categories = productService.getAllCategories();
            model.addAttribute("product", product);
            model.addAttribute("categories", categories);
            model.addAttribute("isEdit", true);
            return "admin/product-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
    }
    /**
     * Processes product save request (insert or update).
     */
    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute("product") Product product,
                              @RequestParam("categoryId") Long categoryId,
                              RedirectAttributes redirectAttributes) {
        try {
            Category category = productService.getCategoryById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
            product.setCategory(category);
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("successMessage", "Product saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
    /**
     * Deletes a product.
     */
    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
    // --- Order Management ---
    /**
     * Lists all store orders for tracking and updating status.
     */
    @GetMapping("/orders")
    public String listOrders(Model model) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);
        model.addAttribute("allStatuses", Order.OrderStatus.values());
        return "admin/orders";
    }
    /**
     * Updates order shipment/completion status from admin table selection.
     */
    @PostMapping("/orders/status")
    public String updateOrderStatus(@RequestParam("orderId") Long orderId,
                                    @RequestParam("status") Order.OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully to: " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating order: " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}
