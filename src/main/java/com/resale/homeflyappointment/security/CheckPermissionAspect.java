package com.resale.homeflyappointment.security;

import com.resale.homeflyappointment.model.Permission;
import com.resale.homeflyappointment.model.UserPermission;
import com.resale.homeflyappointment.repos.PermissionRepository;
import com.resale.homeflyappointment.repos.UserPermissionRepository;
import com.resale.homeflyappointment.shared.PermissionDeniedException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@ControllerAdvice
public class CheckPermissionAspect {

    private final UserPermissionRepository userPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(checkPermission)")
    public Object verifyPermission(ProceedingJoinPoint joinPoint, CheckPermission checkPermission) throws Throwable {

        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        String token = jwt.getTokenValue();

        String tokenType = jwtTokenUtil.extractClaim(
                token,
                c -> c.get("token_type", String.class)
        );

        Integer userId = jwtTokenUtil.extractUserId(token);

        if ("USER".equals(tokenType) && userId == null) {
            throw new SecurityException("Invalid USER token: missing userId");
        }

        if (!"USER".equals(tokenType)) {
            return joinPoint.proceed();
        }

        List<UserPermission> userPerms = userPermissionRepository.findByUserId(userId);
        if (userPerms.isEmpty()) {
            throw new SecurityException("User has no permissions assigned");
        }

        Set<Integer> permIds = userPerms.stream()
                .map(UserPermission::getPermissionId)
                .collect(Collectors.toSet());

        List<Permission> permissions = permissionRepository.findAllById(permIds);
        Set<String> userActions = permissions.stream()
                .map(p -> p.getResource() + ":" + p.getAction())
                .collect(Collectors.toSet());

        // check permissions
        boolean allowed = switch (checkPermission.match()) {
            case ANY ->
                    List.of(checkPermission.value()).stream().anyMatch(userActions::contains);
            case ALL ->
                    userActions.containsAll(List.of(checkPermission.value()));
        };

        if (!allowed) {
            throw new PermissionDeniedException("You do not have the required permission(s) to access this resource.");
        }

        return joinPoint.proceed();
    }
}


