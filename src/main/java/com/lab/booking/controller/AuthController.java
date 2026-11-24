package com.lab.booking.controller;

import com.lab.booking.exception.BusinessRuleException;
import com.lab.booking.model.Role;
import com.lab.booking.model.User;
import com.lab.booking.service.UserService;
import com.lab.booking.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getRole() == Role.ADMINISTRATOR) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                HttpSession session,
                                Model model) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user != null) {
            return user.getRole() == Role.ADMINISTRATOR ? "redirect:/admin/dashboard" : "redirect:/dashboard";
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (error != null) {
            model.addAttribute("errorMessage", error);
        }
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("email") String email,
                              @RequestParam("password") String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = userService.authenticate(email, password);
            SessionUtils.setLoggedInUser(session, user);
            redirectAttributes.addFlashAttribute("successMessage", "Welcome back, " + user.getName() + "!");

            if (user.getRole() == Role.ADMINISTRATOR) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/dashboard";
        } catch (BusinessRuleException e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/quick-login")
    public String handleQuickLogin(@RequestParam("email") String email,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new BusinessRuleException("Demo user not found with email: " + email));
            SessionUtils.setLoggedInUser(session, user);
            redirectAttributes.addFlashAttribute("successMessage", "Logged in as " + user.getName() + " (" + user.getRole() + ")");

            if (user.getRole() == Role.ADMINISTRATOR) {
                return "redirect:/admin/dashboard";
            }
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        SessionUtils.clearLoggedInUser(session);
        return "redirect:/login?logout=true";
    }
}
