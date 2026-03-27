package com.luxferre.chroniqo.service;

import de.focus_shift.jollyday.core.HolidayCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link PublicHolidayService}.
 *
 * <p>Organised in two groups:
 * <ul>
 *   <li>{@code Unit} — mocked {@link CountrySubdivisionRegistry}, tests
 *       caching, null-safety, and delegation logic without calling jollyday.</li>
 *   <li>{@code Integration} — real {@link CountrySubdivisionRegistry} backed
 *       by the classpath {@code iso3166-2.json}, calls jollyday for actual
 *       holiday data. These tests document known German (and other country)
 *       holiday rules for 2026.</li>
 * </ul>
 *
 * @author Luxferre86
 * @since 22.03.2026
 */
class PublicHolidayServiceTest {

    // =========================================================================
    // Unit tests — mocked registry
    // =========================================================================

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Unit — mocked registry")
    class Unit {

        @Mock
        private CountrySubdivisionRegistry registry;

        @InjectMocks
        private PublicHolidayService service;

        // ---------------------------------------------------------------------
        // getHolidays — null / blank guards
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("getHolidays: null countryCode returns empty set immediately")
        void getHolidays_nullCountry_returnsEmpty() {
            Set<LocalDate> result = service.getHolidays(null, null, Year.of(2026));

            assertThat(result).isEmpty();
            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("getHolidays: unsupported country returns empty set and warns")
        void getHolidays_unsupportedCountry_returnsEmpty() {
            when(registry.getCalendar("XX")).thenReturn(Optional.empty());

            Set<LocalDate> result = service.getHolidays("XX", null, Year.of(2026));

            assertThat(result).isEmpty();
            verify(registry).getCalendar("XX");
        }

        // ---------------------------------------------------------------------
        // getHolidays — country code normalisation
        // ---------------------------------------------------------------------

        @ParameterizedTest(name = "countryCode=''{0}'' is normalised to uppercase")
        @ValueSource(strings = {"de", "De", "dE", "DE"})
        @DisplayName("getHolidays: country code is normalised to uppercase before cache lookup")
        void getHolidays_countryCodeNormalisedToUppercase(String code) {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));

            service.getHolidays(code, null, Year.of(2026));

            verify(registry).getCalendar("DE");
        }

        @ParameterizedTest(name = "subdivisionCode=''{0}'' treated as nationwide")
        @NullAndEmptySource
        @DisplayName("getHolidays: null or blank subdivisionCode → nationwide holidays only")
        void getHolidays_nullOrBlankSubdivision_treatedAsNationwide(String sub) {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));

            // The call must not throw and must reach getCalendar
            service.getHolidays("DE", sub, Year.of(2026));

            verify(registry).getCalendar("DE");
            // toJollyDaySubdivision must NOT be called for blank subdivisions
            verify(registry, never()).toJollyDaySubdivision(any());
        }

        @Test
        @DisplayName("getHolidays: non-blank subdivisionCode calls toJollyDaySubdivision")
        void getHolidays_nonBlankSubdivision_callsToJollyDaySubdivision() {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));
            when(registry.toJollyDaySubdivision("DE-BY")).thenReturn("BY");

            service.getHolidays("DE", "DE-BY", Year.of(2026));

            verify(registry).toJollyDaySubdivision("DE-BY");
        }

        @Test
        @DisplayName("getHolidays: subdivisionCode is normalised to uppercase before use")
        void getHolidays_subdivisionCodeNormalisedToUppercase() {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));
            when(registry.toJollyDaySubdivision("DE-BY")).thenReturn("BY");

            service.getHolidays("DE", "de-by", Year.of(2026));

            verify(registry).toJollyDaySubdivision("DE-BY");
        }

        // ---------------------------------------------------------------------
        // Caching
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("getHolidays: same (country, subdivision, year) is only loaded once")
        void getHolidays_sameKey_loadedOnlyOnce() {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));

            service.getHolidays("DE", null, Year.of(2026));
            service.getHolidays("DE", null, Year.of(2026));
            service.getHolidays("DE", null, Year.of(2026));

            // getCalendar is only called once because the result is cached
            verify(registry, times(1)).getCalendar("DE");
        }

        @Test
        @DisplayName("getHolidays: different years produce separate cache entries")
        void getHolidays_differentYears_loadedSeparately() {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));

            service.getHolidays("DE", null, Year.of(2026));
            service.getHolidays("DE", null, Year.of(2027));

            verify(registry, times(2)).getCalendar("DE");
        }

        @Test
        @DisplayName("getHolidays: different subdivisions produce separate cache entries")
        void getHolidays_differentSubdivisions_loadedSeparately() {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));
            when(registry.toJollyDaySubdivision("DE-BY")).thenReturn("BY");
            when(registry.toJollyDaySubdivision("DE-NW")).thenReturn("NW");

            service.getHolidays("DE", "DE-BY", Year.of(2026));
            service.getHolidays("DE", "DE-NW", Year.of(2026));

            verify(registry, times(2)).getCalendar("DE");
        }

        @Test
        @DisplayName("getHolidays: cached result is immutable (returns same Set instance)")
        void getHolidays_cachedResult_returnsSameInstance() {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));

            Set<LocalDate> first = service.getHolidays("DE", null, Year.of(2026));
            Set<LocalDate> second = service.getHolidays("DE", null, Year.of(2026));

            assertThat(first).isSameAs(second);
        }

        // ---------------------------------------------------------------------
        // isHoliday — null guard
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("isHoliday: null countryCode returns false without any registry call")
        void isHoliday_nullCountry_returnsFalse() {
            boolean result = service.isHoliday(LocalDate.of(2026, 1, 1), null, null);

            assertThat(result).isFalse();
            verifyNoInteractions(registry);
        }

        @Test
        @DisplayName("isHoliday: unsupported country returns false")
        void isHoliday_unsupportedCountry_returnsFalse() {
            when(registry.getCalendar("XX")).thenReturn(Optional.empty());

            assertThat(service.isHoliday(
                    LocalDate.of(2026, 1, 1), "XX", null)).isFalse();
        }

        @Test
        @DisplayName("isHoliday: delegates Year extraction from date correctly")
        void isHoliday_usesYearFromDate() {
            when(registry.getCalendar("DE"))
                    .thenReturn(Optional.of(HolidayCalendar.GERMANY));

            // Calling isHoliday for 2027 must load 2027, not 2026
            service.getHolidays("DE", null, Year.of(2026)); // prime cache for 2026
            service.isHoliday(LocalDate.of(2027, 1, 1), "DE", null);

            verify(registry, times(2)).getCalendar("DE"); // once for 2026, once for 2027
        }
    }

    // =========================================================================
    // Integration tests — real registry + real jollyday
    // =========================================================================

    @Nested
    @DisplayName("Integration — live jollyday")
    class Integration {

        private PublicHolidayService service;

        @BeforeEach
        void setUp() {
            CountrySubdivisionRegistry registry = new CountrySubdivisionRegistry();
            registry.init();
            service = new PublicHolidayService(registry);
        }

        // ---------------------------------------------------------------------
        // Null / empty safety
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("null country → empty set, no exception")
        void nullCountry_returnsEmptySet() {
            assertThat(service.getHolidays(null, null, Year.of(2026))).isEmpty();
        }

        @Test
        @DisplayName("unknown country code → empty set, no exception")
        void unknownCountry_returnsEmptySet() {
            assertThat(service.getHolidays("XX", null, Year.of(2026))).isEmpty();
        }

        @Test
        @DisplayName("isHoliday with null country → false")
        void isHoliday_nullCountry_returnsFalse() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 1, 1), null, null)).isFalse();
        }

        // ---------------------------------------------------------------------
        // Germany — nationwide holidays (all 16 states share these)
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("DE: New Year's Day 2026 is a nationwide holiday")
        void de_newYearsDay2026_isNationwide() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 1, 1), "DE", null)).isTrue();
        }

        @Test
        @DisplayName("DE: Christmas Day 2026 is a nationwide holiday")
        void de_christmasDay2026_isNationwide() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 12, 25), "DE", null)).isTrue();
        }

        @Test
        @DisplayName("DE: Second Christmas Day (26 Dec) 2026 is a nationwide holiday")
        void de_secondChristmasDay2026_isNationwide() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 12, 26), "DE", null)).isTrue();
        }

        @Test
        @DisplayName("DE: German Unity Day (3 Oct) 2026 is a nationwide holiday")
        void de_germanUnityDay2026_isNationwide() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 10, 3), "DE", null)).isTrue();
        }

        @Test
        @DisplayName("DE: Labour Day (1 May) 2026 is a nationwide holiday")
        void de_labourDay2026_isNationwide() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 5, 1), "DE", null)).isTrue();
        }

        @Test
        @DisplayName("DE: regular working day (2026-03-02 Monday) is not a holiday")
        void de_regularWorkday_isNotHoliday() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 3, 2), "DE", null)).isFalse();
        }

        // ---------------------------------------------------------------------
        // Germany — state-specific holidays
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("DE-BY: Epiphany (6 Jan) 2026 is a holiday in Bavaria")
        void de_by_epiphany2026_isHolidayInBavaria() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 1, 6), "DE", "DE-BY")).isTrue();
        }

        @Test
        @DisplayName("DE-BE: Epiphany (6 Jan) 2026 is NOT a holiday in Berlin")
        void de_be_epiphany2026_isNotHolidayInBerlin() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 1, 6), "DE", "DE-BE")).isFalse();
        }

        @Test
        @DisplayName("DE-NW: Corpus Christi (4 Jun) 2026 is a holiday in NRW")
        void de_nw_corpusChristi2026_isHolidayInNRW() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 6, 4), "DE", "DE-NW")).isTrue();
        }

        @Test
        @DisplayName("DE-HH: Corpus Christi (4 Jun) 2026 is NOT a holiday in Hamburg")
        void de_hh_corpusChristi2026_isNotHolidayInHamburg() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 6, 4), "DE", "DE-HH")).isFalse();
        }

        @Test
        @DisplayName("DE-BW: Corpus Christi (4 Jun) 2026 is a holiday in Baden-Württemberg")
        void de_bw_corpusChristi2026_isHolidayInBW() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 6, 4), "DE", "DE-BW")).isTrue();
        }

        @Test
        @DisplayName("DE-TH: Reformation Day (31 Oct) 2026 is a holiday in Thuringia")
        void de_th_reformationDay2026_isHolidayInThuringia() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 10, 31), "DE", "DE-TH")).isTrue();
        }

        @Test
        @DisplayName("DE-BY: Reformation Day (31 Oct) 2026 is NOT a holiday in Bavaria")
        void de_by_reformationDay2026_isNotHolidayInBavaria() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 10, 31), "DE", "DE-BY")).isFalse();
        }

        // ---------------------------------------------------------------------
        // Case-insensitivity
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("Country code is case-insensitive (lowercase 'de' works)")
        void de_lowercase_worksSameAsUppercase() {
            LocalDate newYear = LocalDate.of(2026, 1, 1);
            assertThat(service.isHoliday(newYear, "de", null))
                    .isEqualTo(service.isHoliday(newYear, "DE", null));
        }

        @Test
        @DisplayName("Subdivision code is case-insensitive ('de-by' works same as 'DE-BY')")
        void de_by_lowercase_worksSameAsUppercase() {
            LocalDate epiphany = LocalDate.of(2026, 1, 6);
            assertThat(service.isHoliday(epiphany, "de", "de-by"))
                    .isEqualTo(service.isHoliday(epiphany, "DE", "DE-BY"))
                    .isTrue();
        }

        // ---------------------------------------------------------------------
        // Other countries
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("AT: New Year's Day 2026 is a holiday in Austria")
        void at_newYearsDay2026_isHoliday() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 1, 1), "AT", null)).isTrue();
        }

        @Test
        @DisplayName("CH: New Year's Day 2026 is a holiday in Switzerland")
        void ch_newYearsDay2026_isHoliday() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 1, 1), "CH", null)).isTrue();
        }

        @Test
        @DisplayName("GB: Christmas Day 2026 is a holiday in the UK (England)")
        void gb_christmasDay2026_isHoliday() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 12, 25), "GB", null)).isTrue();
        }

        @Test
        @DisplayName("US: Independence Day 2026 is a holiday in the United States")
        void us_independenceDay2026_isHoliday() {
            assertThat(service.isHoliday(
                    LocalDate.of(2026, 7, 4), "US", "NY")).isTrue();
        }

        // ---------------------------------------------------------------------
        // getHolidays result properties
        // ---------------------------------------------------------------------

        @Test
        @DisplayName("getHolidays: returned set is not null and not empty for Germany")
        void getHolidays_germany_returnsNonEmptySet() {
            Set<LocalDate> holidays =
                    service.getHolidays("DE", null, Year.of(2026));
            assertThat(holidays).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("getHolidays: Bavaria has more holidays than the nationwide baseline")
        void getHolidays_bavaria_hasMoreHolidaysThanNationwide() {
            Set<LocalDate> nationwide =
                    service.getHolidays("DE", null, Year.of(2026));
            Set<LocalDate> bavaria =
                    service.getHolidays("DE", "DE-BY", Year.of(2026));
            assertThat(bavaria.size()).isGreaterThan(nationwide.size());
        }

        @Test
        @DisplayName("getHolidays: all dates belong to the requested year")
        void getHolidays_allDatesInRequestedYear() {
            service.getHolidays("DE", null, Year.of(2026))
                    .forEach(date ->
                            assertThat(date.getYear())
                                    .as("Holiday %s should be in 2026", date)
                                    .isEqualTo(2026));
        }

        @Test
        @DisplayName("getHolidays: result is cached — same Set instance returned twice")
        void getHolidays_resultsAreCached() {
            Set<LocalDate> first =
                    service.getHolidays("DE", null, Year.of(2026));
            Set<LocalDate> second =
                    service.getHolidays("DE", null, Year.of(2026));
            assertThat(first).isSameAs(second);
        }

        @Test
        @DisplayName("getHolidays: different years return independent results")
        void getHolidays_differentYears_returnIndependentSets() {
            Set<LocalDate> y2026 = service.getHolidays("DE", null, Year.of(2026));
            Set<LocalDate> y2027 = service.getHolidays("DE", null, Year.of(2027));
            assertThat(y2026).isNotSameAs(y2027);
            // Both should contain holidays (non-empty)
            assertThat(y2026).isNotEmpty();
            assertThat(y2027).isNotEmpty();
        }
    }
}