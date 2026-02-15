package com.resale.homeflyappointment.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final List<String> COOKIE_NAMES = Arrays.asList("CUSTOMER_AUTH_TOKEN", "ADMIN_AUTH_TOKEN", "SALES_AUTH_TOKEN");

    @Override
    public String resolve(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAMES.contains(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}


