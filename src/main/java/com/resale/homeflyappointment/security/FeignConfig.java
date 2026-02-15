package com.resale.homeflyappointment.security;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {

            try {
                var request =
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                                .getRequest();

                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && !authHeader.isBlank()) {
                    template.header("Authorization", authHeader);
                    System.out.println("Forwarding Auth: " + authHeader);

                }

                String cookieHeader = request.getHeader("Cookie");
                if (cookieHeader != null && !cookieHeader.isBlank()) {
                    template.header("Cookie", cookieHeader);
                    System.out.println("Forwarding cookie: " + cookieHeader); 

                }

            } catch (Exception ignored) {
            }
        };
    }
}

