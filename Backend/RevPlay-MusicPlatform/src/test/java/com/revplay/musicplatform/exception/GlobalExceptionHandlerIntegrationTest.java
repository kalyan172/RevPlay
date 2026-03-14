package com.revplay.musicplatform.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.catalog.util.FileStorageService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({ GlobalExceptionHandler.class, ApiResponseBodyAdvice.class,
        ExceptionIntegrationController.class })
@Tag("integration")
class GlobalExceptionHandlerIntegrationTest {

    private static final String BASE_PATH = "/integration-exception";

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private FileStorageService fileStorageService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Test
    @DisplayName("integration not found endpoint returns 404 with api response envelope")
    void integrationNotFoundReturns404Envelope() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("integration-missing"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("integration bad request endpoint returns 400")
    void integrationBadRequestReturns400() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("integration generic endpoint returns 500 unexpected error")
    void integrationGenericReturns500() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}

@RestController
@RequestMapping("/integration-exception")
class ExceptionIntegrationController {

    @GetMapping("/not-found")
    String notFound() {
        throw new ResourceNotFoundException("integration-missing");
    }

    @GetMapping("/bad-request")
    String badRequest() {
        throw new BadRequestException("integration-bad-request");
    }

    @GetMapping("/generic")
    String generic() {
        throw new RuntimeException("integration-boom");
    }
}
