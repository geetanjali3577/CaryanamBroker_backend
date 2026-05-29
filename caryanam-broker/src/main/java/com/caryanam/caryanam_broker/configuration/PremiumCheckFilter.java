package com.caryanam.caryanam_broker.configuration;


import com.caryanam.caryanam_broker.entity.User;
import com.caryanam.caryanam_broker.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PremiumCheckFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    private String currentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String[] statuses = status.split(",");
        return statuses[statuses.length - 1].trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !auth.getPrincipal().equals("anonymousUser")) {

            String email = auth.getName();

            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {

                boolean isPremium = "APPROVED".equalsIgnoreCase(currentStatus(user.getPremiumStatus()));
                request.setAttribute("isPremium", isPremium);
            }
        }

        filterChain.doFilter(request, response);
    }
}