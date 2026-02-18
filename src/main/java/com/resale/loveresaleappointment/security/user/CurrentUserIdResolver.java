package com.resale.loveresaleappointment.security.user;

import com.resale.loveresaleappointment.security.CookieBearerTokenResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserIdResolver implements HandlerMethodArgumentResolver {

    private final JwtUserService jwtUserService;
    private final CookieBearerTokenResolver tokenResolver;

    public CurrentUserIdResolver(JwtUserService jwtUserService, CookieBearerTokenResolver tokenResolver) {
        this.jwtUserService = jwtUserService;
        this.tokenResolver = tokenResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String token = tokenResolver.resolve(request);

        if (token == null || !jwtUserService.validateToken(token)) {
            return null;
        }

        Long id = jwtUserService.getUserId(token);
        System.out.println("Resolved userId from JWT = " + id);
        return jwtUserService.getUserId(token);
    }
}


