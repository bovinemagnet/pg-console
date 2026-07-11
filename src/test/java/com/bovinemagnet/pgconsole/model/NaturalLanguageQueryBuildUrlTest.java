package com.bovinemagnet.pgconsole.model;

import com.bovinemagnet.pgconsole.model.NaturalLanguageQuery.Intent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@code buildUrl} URL-encodes the reflected {@code instance}
 * parameter so an XSS payload cannot survive into the page (M08).
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@DisplayName("NaturalLanguageQuery.buildUrl encoding")
class NaturalLanguageQueryBuildUrlTest {

    @Test
    @DisplayName("An XSS payload in instance is percent-encoded, not reflected raw")
    void instanceIsEncoded() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(Intent.SLOW_QUERIES);

        String url = query.buildUrl("\"><img src=x onerror=alert(1)>");

        assertThat(url).startsWith("/slow-queries?instance=");
        assertThat(url).doesNotContain("<img");
        assertThat(url).doesNotContain("\"");
        assertThat(url).contains("%3Cimg");
    }

    @Test
    @DisplayName("Extracted parameter keys and values are encoded")
    void extractedParametersEncoded() {
        NaturalLanguageQuery query = new NaturalLanguageQuery();
        query.setMatchedIntent(Intent.SLOW_QUERIES);
        query.setExtractedParameters(Map.of("timeRange", "a&b=c"));

        String url = query.buildUrl("prod");

        assertThat(url).contains("instance=prod");
        assertThat(url).contains("timeRange=a%26b%3Dc");
    }
}
