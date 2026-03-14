package com.revplay.musicplatform.catalog.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
class SearchContentTypeTest {

    @ParameterizedTest
    @CsvSource({ "song,SONG", "ALBUM,ALBUM", "artist,ARTIST", "podcast,PODCAST", "all,ALL" })
    @DisplayName("from parses direct values case-insensitively")
    void fromParsesDirectValues(String input, SearchContentType expected) {
        SearchContentType type = SearchContentType.from(input);

        assertThat(type).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "episode", "podcast-episode", "podcastepisode" })
    @DisplayName("from maps episode aliases to PODCAST_EPISODE")
    void fromMapsEpisodeAliases(String input) {
        SearchContentType type = SearchContentType.from(input);

        assertThat(type).isEqualTo(SearchContentType.PODCAST_EPISODE);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "  " })
    @DisplayName("from returns ALL when input is blank")
    void fromReturnsAllForBlank(String input) {
        SearchContentType type = SearchContentType.from(input);

        assertThat(type).isEqualTo(SearchContentType.ALL);
    }

    @ParameterizedTest
    @CsvSource({ "unknown", "video" })
    @DisplayName("from throws validation exception for invalid values")
    void fromThrowsForInvalidValue(String input) {
        assertThatThrownBy(() -> SearchContentType.from(input))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("type must be one of: song, album, artist, podcast, podcast_episode, all");
    }
}
