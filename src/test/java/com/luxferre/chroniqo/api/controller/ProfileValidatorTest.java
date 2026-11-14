package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ProfileValidator")
class ProfileValidatorTest {

    private ProfileValidator validator;

    @BeforeEach
    void setUp() {
        CountrySubdivisionRegistry registry = mock(CountrySubdivisionRegistry.class);
        when(registry.getAllCountries()).thenReturn(Map.of("DE", "Germany", "FR", "France"));
        when(registry.getSubdivisions("DE")).thenReturn(Map.of("DE-BY", "Bavaria", "DE-BE", "Berlin"));
        when(registry.getSubdivisions("FR")).thenReturn(Map.of());
        validator = new ProfileValidator(registry);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void normalizeCountry_blank_returnsNull(String input) {
        assertThat(validator.normalizeCountry(input)).isNull();
    }

    @Test
    void normalizeCountry_trimsAndUpperCases() {
        assertThat(validator.normalizeCountry(" de ")).isEqualTo("DE");
    }

    @Test
    void normalizeCountry_unknown_throwsIllegalArgument() {
        assertThatThrownBy(() -> validator.normalizeCountry("zz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown country: ZZ");
    }

    @Test
    void normalizeSubdivision_withoutCountry_returnsNull() {
        assertThat(validator.normalizeSubdivision(null, "DE-BY")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void normalizeSubdivision_blank_returnsNull(String input) {
        assertThat(validator.normalizeSubdivision("DE", input)).isNull();
    }

    @Test
    void normalizeSubdivision_matchesCaseInsensitivelyAndReturnsRegistryCode() {
        assertThat(validator.normalizeSubdivision("DE", " de-by ")).isEqualTo("DE-BY");
    }

    @Test
    void normalizeSubdivision_notPartOfCountry_throwsIllegalArgument() {
        assertThatThrownBy(() -> validator.normalizeSubdivision("DE", "FR-IDF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown region: FR-IDF");
    }

    @Test
    void normalizeSubdivision_countryWithoutSubdivisions_throwsIllegalArgument() {
        assertThatThrownBy(() -> validator.normalizeSubdivision("FR", "FR-IDF"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
