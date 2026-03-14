package com.revplay.musicplatform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.user.enums.UserRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("unit")
class AuthContextUtilTest {

    private final AuthContextUtil authContextUtil = new AuthContextUtil();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("requireCurrentUserId returns id from authenticated principal")
    void requireCurrentUserIdReturnsId() {
        setAuth(new AuthenticatedUserPrincipal(5L, "user", UserRole.LISTENER), List.of());

        Long userId = authContextUtil.requireCurrentUserId();

        assertThat(userId).isEqualTo(5L);
    }

    @Test
    @DisplayName("requireCurrentUserId throws access denied when unauthenticated")
    void requireCurrentUserIdUnauthenticated() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> authContextUtil.requireCurrentUserId())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentication required");
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull extracts from details map when principal has no id")
    void getCurrentUserIdFromDetailsMap() {
        Object principal = "anonymous";
        Map<String, Object> details = Map.of("user_id", 77L);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        auth.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isEqualTo(77L);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull returns null when authentication is missing")
    void getCurrentUserIdReturnsNullWithoutAuthentication() {
        SecurityContextHolder.clearContext();

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull returns null when authentication exists but is not authenticated")
    void getCurrentUserIdReturnsNullWhenAuthenticationIsNotAuthenticated() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("29", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull extracts numeric principal")
    void getCurrentUserIdFromNumericPrincipal() {
        setAuth(19L, List.of());

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isEqualTo(19L);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull extracts string principal")
    void getCurrentUserIdFromStringPrincipal() {
        setAuth("28", List.of());

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isEqualTo(28L);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull extracts sub claim from principal map")
    void getCurrentUserIdFromSubClaim() {
        setAuth(Map.of("sub", "41"), List.of());

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isEqualTo(41L);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull extracts userId key from details map")
    void getCurrentUserIdFromDetailsUserIdKey() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("anonymous", null, List.of());
        auth.setDetails(Map.of("userId", 88L));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isEqualTo(88L);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull returns null when principal and details do not expose numeric id")
    void getCurrentUserIdReturnsNullForUnsupportedPrincipalAndDetails() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("not-a-number", null, List.of());
        auth.setDetails(Map.of("unknown", "value"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull extracts id from getter accessor")
    void getCurrentUserIdFromGetterAccessor() {
        setAuth(new GetterPrincipal("51", "ADMIN"), List.of());

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isEqualTo(51L);
    }

    @Test
    @DisplayName("getCurrentUserIdOrNull extracts id from record accessor")
    void getCurrentUserIdFromRecordAccessor() {
        setAuth(new RecordStylePrincipal(61L, "LISTENER"), List.of());

        Long userId = authContextUtil.getCurrentUserIdOrNull();

        assertThat(userId).isEqualTo(61L);
    }

    @Test
    @DisplayName("hasRole returns true for ROLE_ prefixed authority")
    void hasRoleWithPrefixedAuthority() {
        setAuth("principal", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        boolean result = authContextUtil.hasRole("ADMIN");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasRole falls back to principal role extractor")
    void hasRoleFromPrincipalRole() {
        setAuth(Map.of("role", "artist"), List.of());

        boolean result = authContextUtil.hasRole("ARTIST");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasRole falls back to record principal role accessor")
    void hasRoleFromRecordPrincipalRole() {
        setAuth(new RecordStylePrincipal(61L, "ADMIN"), List.of());

        boolean result = authContextUtil.hasRole("ADMIN");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasRole returns false when authentication is missing")
    void hasRoleWithoutAuthentication() {
        SecurityContextHolder.clearContext();

        boolean result = authContextUtil.hasRole("ADMIN");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasRole returns false when no authority or principal role matches")
    void hasRoleReturnsFalseForMismatch() {
        setAuth(new GetterPrincipal("7", "listener"), List.of(new SimpleGrantedAuthority("ROLE_ARTIST")));

        boolean result = authContextUtil.hasRole("ADMIN");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasRole returns false when authentication exists but is not authenticated")
    void hasRoleReturnsFalseWhenAuthenticationIsNotAuthenticated() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("principal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        boolean result = authContextUtil.hasRole("ADMIN");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("requireAdmin returns when admin authority exists")
    void requireAdminSucceeds() {
        setAuth("principal", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        authContextUtil.requireAdmin();

        assertThat(authContextUtil.hasRole("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("requireAdmin throws when admin role absent")
    void requireAdminThrows() {
        setAuth(new AuthenticatedUserPrincipal(3L, "listener", UserRole.LISTENER), List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));

        assertThatThrownBy(() -> authContextUtil.requireAdmin())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Admin access required");
    }

    private void setAuth(Object principal, List<SimpleGrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static final class GetterPrincipal {
        private final String userId;
        private final String role;

        private GetterPrincipal(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public String getRole() {
            return role;
        }
    }

    private record RecordStylePrincipal(Long userId, String role) {
    }
}
