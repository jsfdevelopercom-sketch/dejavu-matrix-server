package com.dejavu.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminAuthFilter implements Filter {

    @Value("${ADMIN_API_KEY:dejavu-super-secret-admin-key}")
    private String expectedApiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Apply auth only to admin endpoints
        if (req.getRequestURI().startsWith("/api/admin")) {
            String apiKey = req.getHeader("X-Admin-Key");
            if (apiKey == null || !apiKey.equals(expectedApiKey)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.getWriter().write("{\"error\": \"Unauthorized: Invalid or missing X-Admin-Key header\"}");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}
