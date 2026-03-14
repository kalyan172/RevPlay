package com.revplay.musicplatform.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.revplay.musicplatform.security.service.JwtService;
import com.revplay.musicplatform.security.service.TokenRevocationService;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ SecurityConfig.class, SecurityConfigIntegrationTest.SecurityTestConfig.class })
@Tag("integration")
class SecurityConfigIntegrationTest {

    private static final String PUBLIC_ENDPOINT = "/api/v1/search/internal-security-test";
    private static final String PRIVATE_ENDPOINT = "/api/v1/security-test/private";

    @MockBean
    private JwtService jwtService;
    @MockBean
    private TokenRevocationService tokenRevocationService;
    // JwtAuthenticationFilter is provided by SecurityTestConfig

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("public search endpoint is accessible without authentication")
    void publicEndpointAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get(PUBLIC_ENDPOINT))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("private endpoint returns forbidden without authentication in current security configuration")
    void privateEndpointUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get(PRIVATE_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("private endpoint is accessible with authenticated user")
    void privateEndpointAccessibleWithAuthentication() throws Exception {
        mockMvc.perform(get(PRIVATE_ENDPOINT).with(user("security-user").roles("LISTENER")))
                .andExpect(status().isOk());
    }

    @Configuration
    static class SecurityTestConfig {

        @Bean
        SecurityTestController securityTestController() {
            return new SecurityTestController();
        }

        @Bean(name = "mvcHandlerMappingIntrospector")
        public org.springframework.web.servlet.handler.HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
            return new org.springframework.web.servlet.handler.HandlerMappingIntrospector();
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(null, null) {
                @Override
                protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                        jakarta.servlet.http.HttpServletResponse response,
                        jakarta.servlet.FilterChain filterChain)
                        throws jakarta.servlet.ServletException, java.io.IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }

    @RestController
    @RequestMapping
    static class SecurityTestController {

        @GetMapping(PUBLIC_ENDPOINT)
        String publicEndpoint() {
            return "public";
        }

        @GetMapping(PRIVATE_ENDPOINT)
        String privateEndpoint() {
            return "private";
        }
    }
}
