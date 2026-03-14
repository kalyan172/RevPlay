package com.revplay.musicplatform.catalog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.exception.UnauthorizedException;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.enums.UserRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("unit")
class SecurityUtilTest {

    private final SecurityUtil securityUtil = new SecurityUtil();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getUserId returns id from authenticated principal")
    void getUserIdFromPrincipal() {
        setAuth(new AuthenticatedUserPrincipal(5L, "user", UserRole.LISTENER), List.of());

        Long userId = securityUtil.getUserId();

        assertThat(userId).isEqualTo(5L);
    }

    @Test
    @DisplayName("getUserId resolves id from principal claims map")
    void getUserIdFromClaims() {
        setAuth(Map.of("sub", "15"), List.of(new SimpleGrantedAuthority("ROLE_ARTIST")));

        Long userId = securityUtil.getUserId();

        assertThat(userId).isEqualTo(15L);
    }

    @Test
    @DisplayName("getUserId throws unauthorized when missing")
    void getUserIdMissing() {
        setAuth("anonymousUser", List.of());

        assertThatThrownBy(() -> securityUtil.getUserId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authenticated user not found");
    }

    @Test
    @DisplayName("getUserId resolves numeric userId claim when present")
    void getUserIdFromNumericUserIdClaim() {
        setAuth(Map.of("userId", 22), List.of());

        Long userId = securityUtil.getUserId();

        assertThat(userId).isEqualTo(22L);
    }

    @Test
    @DisplayName("getUserId resolves from getUserId accessor")
    void getUserIdFromGetterAccessor() {
        setAuth(new GetterPrincipal("31"), List.of());

        Long userId = securityUtil.getUserId();

        assertThat(userId).isEqualTo(31L);
    }

    @Test
    @DisplayName("getUserId resolves from userId accessor")
    void getUserIdFromRecordStyleAccessor() {
        setAuth(new RecordStylePrincipal(44L, "listener"), List.of());

        Long userId = securityUtil.getUserId();

        assertThat(userId).isEqualTo(44L);
    }

    @Test
    @DisplayName("getUserId throws unauthorized for unparsable claims")
    void getUserIdInvalidClaim() {
        setAuth(Map.of("userId", "bad-id"), List.of());

        assertThatThrownBy(() -> securityUtil.getUserId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authenticated user not found");
    }

    @Test
    @DisplayName("getUserRole returns normalized role")
    void getUserRoleNormalized() {
        setAuth(Map.of("role", "role_admin"), List.of());

        String role = securityUtil.getUserRole();

        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("getUserRole falls back to authorities")
    void getUserRoleFromAuthority() {
        setAuth("principal", List.of(new SimpleGrantedAuthority("ROLE_ARTIST")));

        String role = securityUtil.getUserRole();

        assertThat(role).isEqualTo("ARTIST");
    }

    @Test
    @DisplayName("getUserRole resolves role from getter accessor")
    void getUserRoleFromGetterAccessor() {
        setAuth(new GetterPrincipal("9", "role_artist"), List.of());

        String role = securityUtil.getUserRole();

        assertThat(role).isEqualTo("ARTIST");
    }

    @Test
    @DisplayName("getUserRole resolves role from record style accessor")
    void getUserRoleFromRecordStyleAccessor() {
        setAuth(new RecordStylePrincipal(12L, "admin"), List.of());

        String role = securityUtil.getUserRole();

        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("getUserRole ignores blank authorities and returns normalized authority")
    void getUserRoleSkipsBlankAuthorities() {
        GrantedAuthority blankAuthority = () -> " ";
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(new Object(), null, List.of(blankAuthority, new SimpleGrantedAuthority("ROLE_LISTENER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        String role = securityUtil.getUserRole();

        assertThat(role).isEqualTo("LISTENER");
    }

    @Test
    @DisplayName("getUserRole throws unauthorized when unavailable")
    void getUserRoleMissing() {
        setAuth("anonymousUser", List.of());

        assertThatThrownBy(() -> securityUtil.getUserRole())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authenticated user role not found");
    }

    private void setAuth(Object principal, List<SimpleGrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static final class GetterPrincipal {
        private final String userId;
        private final String role;

        private GetterPrincipal(String userId) {
            this(userId, null);
        }

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
