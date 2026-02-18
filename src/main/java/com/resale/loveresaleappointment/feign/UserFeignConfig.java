package com.resale.loveresaleappointment.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserFeignConfig implements RequestInterceptor {

    @Value("${internal.auth.token}")
    private String internalAuthToken;

    @Override
    public void apply(RequestTemplate template) {
        // ✅ Automatically include secure internal header
        template.header("X-Internal-Auth", internalAuthToken);
    }
}


