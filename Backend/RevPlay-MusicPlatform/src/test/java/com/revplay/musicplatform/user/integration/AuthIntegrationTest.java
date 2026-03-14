package com.revplay.musicplatform.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.repository.PasswordResetTokenRepository;
import com.revplay.musicplatform.user.repository.UserProfileRepository;
import com.revplay.musicplatform.user.repository.UserRepository;
import com.revplay.musicplatform.user.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AuthIntegrationTest {

    private static final String BASE_AUTH = "/api/v1/auth";
    private static final String PROFILE_BASE = "/api/v1/profile/";
    private static final String EMAIL = "flow@revplay.com";
    private static final String USERNAME = "flowUser";
    private static final String PASSWORD = "StrongPass@123";
    private static final String FULL_NAME = "Flow User";
    private static final String DUP_EMAIL = "dup@revplay.com";
    private static final String DUP_USER_A = "dupA";
    private static final String DUP_USER_B = "dupB";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @MockBean
    private EmailService emailService;
    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Autowired
    AuthIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @BeforeEach
    void clean() {
        passwordResetTokenRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("register verify otp login and access protected endpoint returns 200")
    void registerVerifyLoginProtectedFlow() throws Exception {
        register(EMAIL, USERNAME);
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        verifyOtp(EMAIL, user.getEmailOtp());
        String accessToken = loginAndExtractAccessToken(EMAIL, PASSWORD);

        mockMvc.perform(get(PROFILE_BASE + user.getUserId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("duplicate email registration returns 409")
    void duplicateEmailRegistration() throws Exception {
        register(DUP_EMAIL, DUP_USER_A);
        User firstUser = userRepository.findByEmailIgnoreCase(DUP_EMAIL).orElseThrow();
        verifyOtp(DUP_EMAIL, firstUser.getEmailOtp());

        mockMvc.perform(post(BASE_AUTH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload(DUP_EMAIL, DUP_USER_B)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("login for unverified account returns 401")
    void loginUnverifiedAccount() throws Exception {
        register(EMAIL, USERNAME);
        mockMvc.perform(post(BASE_AUTH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(EMAIL, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login logout then using old access token returns 401")
    void logoutRevokesAccessToken() throws Exception {
        register(EMAIL, USERNAME);
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        verifyOtp(EMAIL, user.getEmailOtp());
        String accessToken = loginAndExtractAccessToken(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE_AUTH + "/logout").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get(PROFILE_BASE + user.getUserId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("login refresh and new access token can access protected endpoint")
    void refreshFlowAndProtectedAccess() throws Exception {
        register(EMAIL, USERNAME);
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        verifyOtp(EMAIL, user.getEmailOtp());
        JsonNode loginNode = loginAndExtractTokens(EMAIL, PASSWORD);
        String refreshToken = loginNode.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post(BASE_AUTH + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode refreshNode = root(refreshResult).path("data");
        String newAccessToken = refreshNode.get("accessToken").asText();

        mockMvc.perform(get(PROFILE_BASE + user.getUserId()).header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("refresh endpoint with access token returns 401")
    void refreshWithAccessTokenFails() throws Exception {
        register(EMAIL, USERNAME);
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        verifyOtp(EMAIL, user.getEmailOtp());
        String accessToken = loginAndExtractAccessToken(EMAIL, PASSWORD);

        mockMvc.perform(post(BASE_AUTH + "/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + accessToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void register(String email, String username) throws Exception {
        mockMvc.perform(post(BASE_AUTH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload(email, username)))
                .andExpect(status().isCreated());
    }

    private void verifyOtp(String email, String otp) throws Exception {
        mockMvc.perform(post(BASE_AUTH + "/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk());
    }

    private String loginAndExtractAccessToken(String email, String password) throws Exception {
        return loginAndExtractTokens(email, password).get("accessToken").asText();
    }

    private JsonNode loginAndExtractTokens(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post(BASE_AUTH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = root(loginResult).path("data");
        assertThat(data.get("accessToken").asText()).isNotBlank();
        assertThat(data.get("refreshToken").asText()).isNotBlank();
        return data;
    }

    private JsonNode root(MvcResult mvcResult) throws Exception {
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString());
    }

    private String registerPayload(String email, String username) {
        return "{\"email\":\"" + email + "\",\"username\":\"" + username + "\",\"password\":\"" + PASSWORD
                + "\",\"fullName\":\"" + FULL_NAME + "\",\"role\":\"LISTENER\"}";
    }

    private String loginPayload(String identifier, String password) {
        return "{\"usernameOrEmail\":\"" + identifier + "\",\"password\":\"" + password + "\"}";
    }
}
