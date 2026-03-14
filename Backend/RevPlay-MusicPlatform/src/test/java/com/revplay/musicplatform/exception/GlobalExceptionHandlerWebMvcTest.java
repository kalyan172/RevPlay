package com.revplay.musicplatform.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.security.service.JwtService;
import com.revplay.musicplatform.security.service.TokenRevocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = TestExceptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class GlobalExceptionHandlerWebMvcTest {

    private static final String BASE_PATH = "/test-exception";

    private final org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    GlobalExceptionHandlerWebMvcTest(org.springframework.test.web.servlet.MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @org.springframework.boot.test.mock.mockito.MockBean
    private FileStorageProperties fileStorageProperties;
    @org.springframework.boot.test.mock.mockito.MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @org.springframework.boot.test.mock.mockito.MockBean
    private JwtService jwtService;
    @org.springframework.boot.test.mock.mockito.MockBean
    private TokenRevocationService tokenRevocationService;

    @Test
    @DisplayName("resource not found is mapped to 404 response")
    void notFoundMapsTo404() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("duplicate resource is mapped to 409 response")
    void duplicateMapsTo409() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("method argument validation is mapped to 400 with errors list")
    void methodValidationMapsTo400() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    @DisplayName("type mismatch is mapped to 400 with descriptive message")
    void typeMismatchMapsTo400() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/type/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid value 'abc'")));
    }

    @Test
    @DisplayName("generic exception is mapped to 500 unexpected error")
    void genericMapsTo500() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}

@RestController
@RequestMapping("/test-exception")
class TestExceptionController {

    @GetMapping("/not-found")
    String notFound() {
        throw new ResourceNotFoundException("missing");
    }

    @GetMapping("/duplicate")
    String duplicate() {
        throw new DuplicateResourceException("duplicate");
    }

    @GetMapping("/type/{songId}")
    String typeMismatch(@PathVariable Integer songId) {
        return songId.toString();
    }

    @PostMapping("/validate")
    String validate(@Valid @RequestBody ValidationRequest request) {
        return request.name();
    }

    @GetMapping("/generic")
    String generic() {
        throw new RuntimeException("boom");
    }
}

record ValidationRequest(@NotBlank String name) {
}
