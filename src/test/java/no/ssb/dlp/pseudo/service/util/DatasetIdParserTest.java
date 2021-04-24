package no.ssb.dlp.pseudo.service.util;

import no.ssb.dapla.dataset.api.DatasetId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static no.ssb.dlp.pseudo.service.util.DatasetIdParser.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DatasetIdParserTest {

    @Test
    public void givenPathWithoutVersion_whenParse_thenCreateDatasetIdWithCurrentTimestampAsVersion() {
        DatasetId ds = parse("/path/to/dataset");
        assertThat(ds.getPath()).isEqualTo("/path/to/dataset");
        assertApproximateNow(ds.getVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1619242421337", "1619242421337@abc", "blah", "    versionSurroundedBySpaces    "})
    public void givenPathWithVersion_whenParse_thenCreateDatasetIdWithSpecifiedVersion(String version) {
        DatasetId ds = parse("/path/to/dataset@" + version);
        assertThat(ds.getPath()).isEqualTo("/path/to/dataset");
        assertThat(ds.getVersion()).isEqualTo(version.trim());
    }

    @Test
    public void givenPathWithEmptyVersion_whenParse_thenCreateDatasetIdWithCurrentTimestampAsVersion() {
        DatasetId ds = parse("/path/to/dataset@");
        assertThat(ds.getPath()).isEqualTo("/path/to/dataset");
        assertApproximateNow(ds.getVersion());
    }

    @Test
    public void givenNull_whenParse_thenThrowException() {
        assertThatExceptionOfType(DatasetIdParser.ParseException.class)
          .isThrownBy(() -> {
              parse(null);
          }).withMessage("dataset path cannot be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "\t", "\n", "        \t\n      "})
    public void givenBlankOrEmptyPath_whenParse_thenThrowException(String path) {
        assertThatExceptionOfType(DatasetIdParser.ParseException.class)
          .isThrownBy(() -> {
              parse(path);
          }).withMessage("dataset path cannot be empty");
    }

    /**
     * Assert that timestamp is within the last second
     */
    private static void assertApproximateNow(String timestamp) {
        assertThat(Instant.now().toEpochMilli() - Long.parseLong(timestamp)).isLessThan(1000);
    }

}