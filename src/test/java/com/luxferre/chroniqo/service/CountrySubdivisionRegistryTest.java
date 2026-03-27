package com.luxferre.chroniqo.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CountrySubdivisionRegistryTest {

    private CountrySubdivisionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CountrySubdivisionRegistry();
        registry.init();
    }

    // =========================================================================
    // 🌍 Countries
    // =========================================================================

    @Nested
    class Countries {

        @Test
        void shouldLoadCountries() {
            assertThat(registry.getAllCountries())
                    .isNotEmpty()
                    .containsKey("DE");
        }

        @Test
        void shouldBeSortedAlphabetically() {
            assertThat(new ArrayList<>(registry.getAllCountries().values())).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        }

        @Test
        void keysShouldBeUppercase() {
            registry.getAllCountries().keySet()
                    .forEach(code ->
                            assertThat(code).isEqualTo(code.toUpperCase()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"de", "De", "DE"})
        void lookupShouldBeCaseInsensitive(String input) {
            assertThat(registry.getCountryDisplayName(input))
                    .isEqualTo("Germany");
        }

        @Test
        void shouldFallbackToCodeIfUnknown() {
            assertThat(registry.getCountryDisplayName("XX"))
                    .isEqualTo("XX");
        }

        @Test
        void shouldReturnEmptyForNull() {
            assertThat(registry.getCountryDisplayName(null)).isEmpty();
        }
    }

    // =========================================================================
    // 🗺️ Subdivisions
    // =========================================================================

    @Nested
    class Subdivisions {

        @Test
        void shouldReturnSubdivisions() {
            Map<String, String> subs = registry.getSubdivisions("DE");

            assertThat(subs)
                    .isNotEmpty()
                    .containsKey("DE-BY");
        }

        @Test
        void shouldBeSortedAlphabetically() {
            assertThat(new ArrayList<>(registry.getSubdivisions("DE").values())).isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        }

        @Test
        void keysShouldBeUppercase() {
            registry.getSubdivisions("DE").keySet()
                    .forEach(code ->
                            assertThat(code).isEqualTo(code.toUpperCase()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"de-by", "DE-BY", "De-By"})
        void lookupShouldBeCaseInsensitive(String input) {
            assertThat(registry.getSubdivisionDisplayName("DE", input)).isEqualTo("Bayern");
        }

        @Test
        void shouldFallbackIfUnknown() {
            assertThat(registry.getSubdivisionDisplayName("DE", "DE-XXX")).isEqualTo("DE-XXX");
        }

        @Test
        void shouldReturnEmptyIfNull() {
            assertThat(registry.getSubdivisionDisplayName(null, "DE-BY")).isEqualTo("DE-BY");
            assertThat(registry.getSubdivisionDisplayName("DE", null)).isEmpty();
        }

        @Test
        void shouldReturnEmptyMapForUnknownCountry() {
            assertThat(registry.getSubdivisions("XX")).isEmpty();
        }

        @Test
        void shouldReturnEmptyMapForNullCountry() {
            assertThat(registry.getSubdivisions(null)).isEmpty();
        }
    }

    // =========================================================================
    // 🎄 Jollyday
    // =========================================================================

    @Nested
    class Jollyday {

        @Test
        void shouldDetectSupportedCountry() {
            assertThat(registry.hasHolidaySupport("DE")).isTrue();
        }

        @Test
        void shouldReturnFalseForUnsupportedCountry() {
            assertThat(registry.hasHolidaySupport("XX")).isFalse();
        }

        @Test
        void shouldReturnFalseForNull() {
            assertThat(registry.hasHolidaySupport(null)).isFalse();
        }

        @Test
        void shouldReturnCalendarIfSupported() {
            assertThat(registry.getCalendar("DE")).isPresent();
        }

        @Test
        void shouldReturnEmptyIfUnsupported() {
            assertThat(registry.getCalendar("XX")).isEmpty();
        }

        @Test
        void shouldReturnEmptyIfNull() {
            assertThat(registry.getCalendar(null)).isEmpty();
        }

        @Test
        void consistencyBetweenMethods() {
            registry.getAllCountries().keySet().forEach(code -> {
                boolean hasSupport = registry.hasHolidaySupport(code);
                boolean hasCalendar = registry.getCalendar(code).isPresent();

                assertThat(hasSupport).isEqualTo(hasCalendar);
            });
        }
    }

    // =========================================================================
    // 🔄 ISO Conversion
    // =========================================================================

    @Nested
    class IsoConversion {

        @ParameterizedTest
        @ValueSource(strings = {"DE-BY", "de-by", "De-By"})
        void shouldExtractSubdivision(String input) {
            assertThat(registry.toJollyDaySubdivision(input))
                    .isEqualTo("BY");
        }

        @Test
        void shouldReturnSameIfNoDash() {
            assertThat(registry.toJollyDaySubdivision("DE"))
                    .isEqualTo("DE");
        }

        @Test
        void shouldHandleMultipleDashes() {
            assertThat(registry.toJollyDaySubdivision("GB-ENG-LON"))
                    .isEqualTo("ENG-LON");
        }

        @Test
        void shouldHandleLeadingDash() {
            assertThat(registry.toJollyDaySubdivision("-BY"))
                    .isEqualTo("BY");
        }

        @Test
        void shouldHandleWeirdInput() {
            assertThat(registry.toJollyDaySubdivision("----"))
                    .isEqualTo("---");
        }

        @Test
        void shouldReturnEmptyForNull() {
            assertThat(registry.toJollyDaySubdivision(null)).isEmpty();
        }
    }

    // =========================================================================
    // 🧪 Robustness / Edge Cases
    // =========================================================================

    @Nested
    class EdgeCases {

        @Test
        void shouldNotThrowOnCompletelyInvalidInput() {
            assertThat(registry.getCountryDisplayName("!!!")).isEqualTo("!!!");
            assertThat(registry.getSubdivisions("???")).isEmpty();
        }

        @Test
        void repeatedCallsShouldBeStable() {
            Map<String, String> first = registry.getSubdivisions("DE");
            Map<String, String> second = registry.getSubdivisions("DE");

            assertThat(first).isSameAs(second);
        }

        @Test
        void mapsShouldBeImmutable() {
            Map<String, String> countries = registry.getAllCountries();

            Assertions.assertThrows(UnsupportedOperationException.class,
                    () -> countries.put("XX", "Test"));
        }

        @Test
        void subdivisionMapsShouldBeImmutable() {
            Map<String, String> subs = registry.getSubdivisions("DE");

            Assertions.assertThrows(UnsupportedOperationException.class,
                    () -> subs.put("XX", "Test"));
        }
    }
}