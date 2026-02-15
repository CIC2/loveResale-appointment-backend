package com.resale.homeflyappointment.security.customer;

import com.resale.homeflyappointment.security.CookieBearerTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentCustomerIdResolver implements HandlerMethodArgumentResolver {

    private final JwtCustomerService jwtCustomerService;
    private final CookieBearerTokenResolver tokenResolver;

    public CurrentCustomerIdResolver(JwtCustomerService jwtCustomerService, CookieBearerTokenResolver tokenResolver) {
        this.jwtCustomerService = jwtCustomerService;
        this.tokenResolver = tokenResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentCustomerId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String token = tokenResolver.resolve(request);

        if (token == null || !jwtCustomerService.validateToken(token)) {
            return null;
        }

        Long id = jwtCustomerService.getCustomerId(token);
        System.out.println("Resolved customerId from JWT = " + id);
        return jwtCustomerService.getCustomerId(token);
    }
}


