package com.revplay.musicplatform.artist.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.artist.repository.ArtistRepository;
import com.revplay.musicplatform.artist.repository.ArtistSocialLinkRepository;
import com.revplay.musicplatform.catalog.service.DiscoveryPerformanceService;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.user.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class ArtistIntegrationTest {

    private static final String BASE = "/api/v1/artists";
    private static final String JSON = "application/json";
    private static final Long ARTIST_USER_ID = 4001L;
    private static final Long OTHER_USER_ID = 4002L;
    private static final Long ADMIN_USER_ID = 4999L;
    private static final String CREATE_ARTIST_BODY = """
            {
              "displayName":"Artist One",
              "bio":"Bio",
              "bannerImageUrl":"banner",
              "artistType":"MUSIC"
            }
            """;
    private static final String UPDATE_ARTIST_BODY = """
            {
              "displayName":"Artist Updated",
              "bio":"Updated Bio",
              "bannerImageUrl":"banner2",
              "artistType":"PODCAST"
            }
            """;
    private static final String VERIFY_TRUE_BODY = "{\"verified\":true}";
    private static final String LINK_CREATE_BODY = """
            {
              "platform":"INSTAGRAM",
              "url":"https://instagram.com/artist"
            }
            """;
    private static final String LINK_UPDATE_BODY = """
            {
              "platform":"YOUTUBE",
              "url":"https://youtube.com/artist"
            }
            """;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final ArtistRepository artistRepository;
    private final ArtistSocialLinkRepository artistSocialLinkRepository;

    @MockBean
    private DiscoveryPerformanceService discoveryPerformanceService;

    @Autowired
    ArtistIntegrationTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            ArtistRepository artistRepository,
            ArtistSocialLinkRepository artistSocialLinkRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.artistRepository = artistRepository;
        this.artistSocialLinkRepository = artistSocialLinkRepository;
    }

    @BeforeEach
    void clean() {
        artistSocialLinkRepository.deleteAll();
        artistRepository.deleteAll();
    }

    @Test
    @DisplayName("artist create update get summary and verify flow succeeds")
    void artistCoreFlow() throws Exception {
        Long artistId = createArtist(ARTIST_USER_ID);

        mockMvc.perform(put(BASE + "/" + artistId)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST)))
                        .contentType(JSON)
                        .content(UPDATE_ARTIST_BODY))
                .andExpect(status().isOk());

        MvcResult getResult = mockMvc.perform(get(BASE + "/" + artistId)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode getRoot = root(getResult);
        assertThat(getRoot.path("data").path("displayName").asText()).isEqualTo("Artist Updated");

        MvcResult summaryResult = mockMvc.perform(get(BASE + "/" + artistId + "/summary")
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode summaryRoot = root(summaryResult);
        assertThat(summaryRoot.path("data").path("songCount").asLong()).isZero();

        mockMvc.perform(patch(BASE + "/" + artistId + "/verify")
                        .with(authentication(auth(ADMIN_USER_ID, UserRole.ADMIN)))
                        .contentType(JSON)
                        .content(VERIFY_TRUE_BODY))
                .andExpect(status().isOk());

        MvcResult verifiedResult = mockMvc.perform(get(BASE + "/" + artistId)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(root(verifiedResult).path("data").path("verified").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("artist social link create list update delete flow succeeds")
    void artistSocialLinkFlow() throws Exception {
        Long artistId = createArtist(ARTIST_USER_ID);
        String socialBase = BASE + "/" + artistId + "/social-links";

        MvcResult createLink = mockMvc.perform(post(socialBase)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST)))
                        .contentType(JSON)
                        .content(LINK_CREATE_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        Long linkId = root(createLink).path("data").path("linkId").asLong();

        MvcResult listResult = mockMvc.perform(get(socialBase)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listRoot = root(listResult);
        assertThat(listRoot.path("data").size()).isEqualTo(1);

        mockMvc.perform(put(socialBase + "/" + linkId)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST)))
                        .contentType(JSON)
                        .content(LINK_UPDATE_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(delete(socialBase + "/" + linkId)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST))))
                .andExpect(status().isOk());

        MvcResult listAfterDelete = mockMvc.perform(get(socialBase)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(root(listAfterDelete).path("data").size()).isZero();
    }

    @Test
    @DisplayName("duplicate artist profile for same user returns conflict")
    void duplicateArtistConflict() throws Exception {
        createArtist(ARTIST_USER_ID);

        mockMvc.perform(post(BASE)
                        .with(authentication(auth(ARTIST_USER_ID, UserRole.ARTIST)))
                        .contentType(JSON)
                        .content(CREATE_ARTIST_BODY))
                .andExpect(status().isConflict());

        assertThat(artistRepository.findByUserId(ARTIST_USER_ID)).isPresent();
    }

    @Test
    @DisplayName("non owner cannot update artist and receives not found")
    void nonOwnerCannotUpdateArtist() throws Exception {
        Long artistId = createArtist(ARTIST_USER_ID);

        mockMvc.perform(put(BASE + "/" + artistId)
                        .with(authentication(auth(OTHER_USER_ID, UserRole.ARTIST)))
                        .contentType(JSON)
                        .content(UPDATE_ARTIST_BODY))
                .andExpect(status().isNotFound());

        assertThat(artistRepository.findById(artistId)).isPresent();
    }

    @Test
    @DisplayName("artist endpoints require authentication")
    void artistEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE).contentType(JSON).content(CREATE_ARTIST_BODY)).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/1/summary")).andExpect(status().isForbidden());

        assertThat(artistRepository.count()).isZero();
    }

    private Long createArtist(Long userId) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .with(authentication(auth(userId, UserRole.ARTIST)))
                        .contentType(JSON)
                        .content(CREATE_ARTIST_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        return root(result).path("data").path("artistId").asLong();
    }

    private JsonNode root(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UsernamePasswordAuthenticationToken auth(Long userId, UserRole role) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(userId, "u" + userId, role),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
    }
}
