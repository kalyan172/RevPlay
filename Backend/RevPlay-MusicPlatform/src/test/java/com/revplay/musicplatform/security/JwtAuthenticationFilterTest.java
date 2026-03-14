package com.revplay.musicplatform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.security.service.JwtService;
import com.revplay.musicplatform.security.service.TokenRevocationService;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.exception.AuthUnauthorizedException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class JwtAuthenticationFilterTest {

    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String REFRESH_TOKEN = "valid-refresh-token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Mock
    private JwtService jwtService;
    @Mock
    private TokenRevocationService tokenRevocationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("invalidAuthHeaders")
    @DisplayName("invalid or missing authorization header leaves security context unauthenticated")
    void invalidAuthorizationHeaders(String headerValue, int expectedAccessTokenChecks) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        if (headerValue != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, headerValue);
        }

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        if (expectedAccessTokenChecks == 0) {
            verify(jwtService, never()).isAccessToken(org.mockito.ArgumentMatchers.anyString());
            return;
        }
        verify(jwtService, times(expectedAccessTokenChecks)).isAccessToken("");
    }

    @Test
    @DisplayName("valid non-revoked access token sets authenticated principal")
    void validAccessTokenSetsContext() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(tokenRevocationService.isRevoked(ACCESS_TOKEN)).thenReturn(false);
        when(jwtService.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtService.toPrincipal(ACCESS_TOKEN)).thenReturn(new AuthenticatedUserPrincipal(1L, "listener", UserRole.ARTIST));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo(UserRole.ARTIST);
    }

    @Test
    @DisplayName("revoked token keeps security context empty and continues chain")
    void revokedTokenSkipped() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(tokenRevocationService.isRevoked(ACCESS_TOKEN)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("refresh token used as bearer token does not authenticate")
    void refreshTokenAsAccessRejected() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + REFRESH_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(tokenRevocationService.isRevoked(REFRESH_TOKEN)).thenReturn(false);
        when(jwtService.isAccessToken(REFRESH_TOKEN)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("already authenticated request still invokes JWT service in current implementation")
    void alreadyAuthenticatedStillCallsJwtService() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("existing", null));
        when(tokenRevocationService.isRevoked(ACCESS_TOKEN)).thenReturn(false);
        when(jwtService.isAccessToken(ACCESS_TOKEN)).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(jwtService).isAccessToken(ACCESS_TOKEN);
    }

    @Test
    @DisplayName("lowercase bearer is treated as invalid due case-sensitive prefix check")
    void lowercaseBearerIsInvalid() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("jwt parsing exceptions propagate from filter in current implementation")
    void jwtExceptionsPropagate() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(tokenRevocationService.isRevoked(ACCESS_TOKEN)).thenReturn(false);
        when(jwtService.isAccessToken(ACCESS_TOKEN)).thenThrow(new AuthUnauthorizedException("Invalid or expired token"));

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(AuthUnauthorizedException.class);
    }

    @Test
    @DisplayName("invalid role claim from principal mapping propagates exception in current implementation")
    void invalidRoleClaimPropagates() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + ACCESS_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(tokenRevocationService.isRevoked(ACCESS_TOKEN)).thenReturn(false);
        when(jwtService.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtService.toPrincipal(ACCESS_TOKEN)).thenThrow(new AuthUnauthorizedException("Invalid token role claim"));

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Invalid token role claim");
    }

    private static Stream<Arguments> invalidAuthHeaders() {
        return Stream.of(
                Arguments.of((String) null, 0),
                Arguments.of("", 0),
                Arguments.of("Bearer ", 1),
                Arguments.of("Basic dXNlcjpwYXNz", 0),
                Arguments.of("Bearer", 0)
        );
    }
}
