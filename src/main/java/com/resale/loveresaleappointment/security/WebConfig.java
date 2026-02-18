package com.resale.loveresaleappointment.security;

import com.resale.loveresaleappointment.security.customer.CurrentCustomerIdResolver;
import com.resale.loveresaleappointment.security.user.CurrentUserIdResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserIdResolver currentUserIdResolver;
    private final CurrentCustomerIdResolver currentCustomerIdResolver;

    public WebConfig(CurrentUserIdResolver currentCustomerIdResolver, CurrentCustomerIdResolver currentCustomerIdResolver1) {
        this.currentUserIdResolver = currentCustomerIdResolver;
        this.currentCustomerIdResolver = currentCustomerIdResolver1;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdResolver);
        resolvers.add(currentCustomerIdResolver);
    }
}

