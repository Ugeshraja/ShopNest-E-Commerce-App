package com.shopnest.controller;

import com.shopnest.dto.RegisterDTO;
import com.shopnest.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Controller handling user registration and login views.
 */
@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Shows the login page. Redirects to products page if already authenticated.
     */
    @GetMapping("/login")
    public String showLoginPage(Principal principal) {
        if (principal != null) {
            return "redirect:/products";
        }
        return "auth/login";
    }

    /**
     * Shows the registration page. Redirects to products page if already authenticated.
     */
    @GetMapping("/register")
    public String showRegistrationPage(Model model, Principal principal) {
        if (principal != null) {
            return "redirect:/products";
        }
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    /**
     * Processes registration form submission.
     */
    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        // Validate password match
        if (registerDTO.getPassword() != null && !registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.registerDTO", "Passwords do not match");
        }

        // Validate email uniqueness
        if (registerDTO.getEmail() != null && userService.isEmailRegistered(registerDTO.getEmail())) {
            bindingResult.rejectValue("email", "error.registerDTO", "An account with this email already exists");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(registerDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An error occurred during registration: " + e.getMessage());
            return "auth/register";
        }
    }
}
