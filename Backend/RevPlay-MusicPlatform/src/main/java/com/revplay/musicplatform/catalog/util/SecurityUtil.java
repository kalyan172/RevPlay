package com.revplay.musicplatform.catalog.util;

import com.revplay.musicplatform.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Component
public class SecurityUtil {
    private static final Logger log = LoggerFactory.getLogger(SecurityUtil.class);

    public SecurityUtil() {
    }

    public Long getUserId() {
        Long securityContextUserId = getUserIdFromSecurityContext();
        if (securityContextUserId == null) {
            throw new UnauthorizedException("Authenticated user not found");
        }
        return securityContextUserId;
    }

    public String getUserRole() {
        String securityContextRole = getRoleFromSecurityContext();
        if (securityContextRole == null) {
            throw new UnauthorizedException("Authenticated user role not found");
        }
        return normalizeRole(securityContextRole);
    }

    private Long getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }

        if (principal instanceof String) {
            return null;
        }

        if (principal instanceof Map<?, ?> claims) {
            Object claimUserId = claims.get("userId");
            if (claimUserId == null) {
                claimUserId = claims.get("sub");
            }
            Long parsed = parseLong(claimUserId);
            if (parsed != null) {
                return parsed;
            }
        }

        Long value = invokeLongAccessor(principal, "getUserId");
        if (value != null) {
            return value;
        }
        return invokeLongAccessor(principal, "userId");
    }

    private Long invokeLongAccessor(Object principal, String methodName) {
        try {
            Method method = principal.getClass().getMethod(methodName);
            Object value = method.invoke(principal);
            return parseLong(value);
        } catch (ReflectiveOperationException ex) {
            log.debug("Principal accessor {} unavailable on {}", methodName, principal.getClass().getName());
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Long userId) {
            return userId;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                log.debug("Unable to parse user id from principal claim: {}", text);
            }
        }
        return null;
    }

    private String getRoleFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Map<?, ?> claims) {
            Object roleClaim = claims.get("role");
            if (roleClaim != null) {
                return roleClaim.toString();
            }
        }

        if (principal != null && !(principal instanceof String)) {
            String role = invokeStringAccessor(principal, "getRole");
            if (role != null) {
                return role;
            }
            role = invokeStringAccessor(principal, "role");
            if (role != null) {
                return role;
            }
        }

        return authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(authority -> authority != null && !authority.isBlank())
            .findFirst()
            .orElse(null);
    }

    private String invokeStringAccessor(Object principal, String methodName) {
        try {
            Method method = principal.getClass().getMethod(methodName);
            Object value = method.invoke(principal);
            return value != null ? value.toString() : null;
        } catch (ReflectiveOperationException ex) {
            log.debug("Principal accessor {} unavailable on {}", methodName, principal.getClass().getName());
            return null;
        }
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return "";
        }
        String role = rawRole.trim().toUpperCase();
        if (role.startsWith("ROLE_")) {
            return role.substring(5);
        }
        return role;
    }
}
