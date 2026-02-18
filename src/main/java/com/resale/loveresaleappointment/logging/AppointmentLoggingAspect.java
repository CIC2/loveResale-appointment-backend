package com.resale.loveresaleappointment.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resale.loveresaleappointment.model.ActionType;
import com.resale.loveresaleappointment.model.AppointmentExceptionLog;
import com.resale.loveresaleappointment.model.AppointmentLog;
import com.resale.loveresaleappointment.repos.AppointmentExceptionLogRepository;
import com.resale.loveresaleappointment.repos.AppointmentLogRepository;
import com.resale.loveresaleappointment.security.CookieBearerTokenResolver;
import com.resale.loveresaleappointment.security.JwtTokenUtil;
import com.resale.loveresaleappointment.utils.RequestBodyCachingFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AppointmentLoggingAspect {

    private final AppointmentLogRepository appointmentLogRepository;
    private final AppointmentExceptionLogRepository appointmentExceptionLogRepository;
    private final HttpServletRequest request;
    private final JwtTokenUtil jwtTokenUtil;
    private final CookieBearerTokenResolver cookieBearerTokenResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(logActivity)")
    public Object log(ProceedingJoinPoint joinPoint, LogActivity logActivity) throws Throwable {

        ActionType actionType = logActivity.value();
        AppointmentLog appointmentLog = new AppointmentLog();
        AppointmentExceptionLog appointmentExceptionLog = new AppointmentExceptionLog();
        long start = System.currentTimeMillis();
        String httpMethod = request.getMethod();

        String identityType = "GUEST";
        Integer identityId = null;
        String token = null;

        try {
            token = cookieBearerTokenResolver.resolve(request);
        } catch (Exception ignored) {
        }

        if (token != null) {
            try {
                Integer customerId = jwtTokenUtil.extractCustomerId(token);
                if (customerId != null) {
                    identityType = "CUSTOMER";
                    identityId = customerId;
                }
            } catch (Exception ignored) {
            }

            if (identityId == null) {
                try {
                    Integer userId = jwtTokenUtil.extractUserId(token);
                    if (userId != null) {
                        identityType = "USER";
                        identityId = userId;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (identityId == null) {
            try {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
                    Integer customerId = jwt.getClaim("customerId");
                    Integer userId = jwt.getClaim("userId");

                    if (customerId != null) {
                        identityType = "CUSTOMER";
                        identityId = customerId;
                    } else if (userId != null) {
                        identityType = "USER";
                        identityId = userId;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        int actionCode = actionType.getCode();
        String actionName = actionType.name();

        String requestBodyJson = null;
        if (!"GET".equalsIgnoreCase(httpMethod)) {
            requestBodyJson = extractRequestBody();
        }

        String headersJson = extractHeaders();
        String paramsJson = extractQueryParams();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - start;

            String responseJson = null;
            try {
                responseJson = objectMapper.writeValueAsString(result);
            } catch (Exception ignored) {
            }

            int status = 200;
            if (result instanceof ResponseEntity<?> res) {
                status = res.getStatusCode().value();
            }

            appointmentLog.setIdentityType(identityType);
            appointmentLog.setIdentityId(identityId);
            appointmentLog.setActionCode(actionCode);
            appointmentLog.setActionName(actionName);
            appointmentLog.setHttpMethod(httpMethod);
            appointmentLog.setStatusCode(status);
            appointmentLog.setRequestBody(requestBodyJson);
            appointmentLog.setResponseBody(responseJson);
            appointmentLog.setExecutionTimeMs(executionTime);
            appointmentLog.setCreatedAt(LocalDateTime.now());
            appointmentLog.setHeaders(headersJson);
            appointmentLog.setQueryParams(paramsJson);

            appointmentLogRepository.save(appointmentLog);

            return result;

        } catch (Exception ex) {

            appointmentExceptionLog.setIdentityType(identityType);
            appointmentExceptionLog.setIdentityId(identityId);
            appointmentExceptionLog.setActionCode(actionCode);
            appointmentExceptionLog.setActionName(actionName);
            appointmentExceptionLog.setHttpMethod(httpMethod);
            appointmentExceptionLog.setExceptionType(ex.getClass().getSimpleName());
            appointmentExceptionLog.setMessage(ex.getMessage());
            appointmentExceptionLog.setStacktrace(getStackTrace(ex));
            appointmentExceptionLog.setCreatedAt(LocalDateTime.now());
            appointmentExceptionLog.setHeaders(headersJson);
            appointmentExceptionLog.setQueryParams(paramsJson);

            appointmentExceptionLogRepository.save(appointmentExceptionLog);

            throw ex;
        }
    }

    private String extractHeaders() {
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            Enumeration<String> names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name, request.getHeader(name));
            }
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractQueryParams() {
        try {
            return objectMapper.writeValueAsString(request.getParameterMap());
        } catch (Exception e) {
            return null;
        }
    }

    private String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : t.getStackTrace()) {
            sb.append(ste).append("\n");
        }
        return sb.toString();
    }


    private String extractRequestBody() {
        try {
            Object cached = request.getAttribute(RequestBodyCachingFilter.CACHED_REQUEST);

            if (!(cached instanceof ContentCachingRequestWrapper wrapper)) {
                return null;
            }

            byte[] content = wrapper.getContentAsByteArray();
            if (content.length == 0) {
                return null;
            }

            return new String(content, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return null;
        }
    }
}


