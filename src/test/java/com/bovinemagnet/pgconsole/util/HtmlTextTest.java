package com.bovinemagnet.pgconsole.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextTest {

    @Test
    void escapesTagCharacters() {
        assertThat(HtmlText.escape("<script>alert(1)</script>"))
                .isEqualTo("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void escapesAmpersandAndQuotes() {
        assertThat(HtmlText.escape("Tom & Jerry's \"big\" adventure"))
                .isEqualTo("Tom &amp; Jerry&#39;s &quot;big&quot; adventure");
    }

    @Test
    void nullBecomesEmptyString() {
        assertThat(HtmlText.escape(null)).isEmpty();
    }

    @Test
    void nonStringValuesAreStringifiedThenEscaped() {
        assertThat(HtmlText.escape(42)).isEqualTo("42");
    }
}
