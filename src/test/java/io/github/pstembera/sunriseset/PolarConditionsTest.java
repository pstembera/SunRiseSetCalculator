package io.github.pstembera.sunriseset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the days on which the Sun never crosses the horizon.
 */
class PolarConditionsTest {

    private static final ZoneId TROMSO = ZoneId.of("Europe/Oslo");

    @Test
    @DisplayName("Tromso in June: midnight sun")
    void tromsoMidnightSun() {
        SunTimes times = SolarCalculator.compute(
                new GeoPoint(69.6492, 18.9553), LocalDate.of(2026, 6, 21), TROMSO);

        assertEquals(DayState.POLAR_DAY, times.state());
        assertTrue(times.sunriseTime().isEmpty());
        assertTrue(times.sunsetTime().isEmpty());
        assertEquals(Duration.ofDays(1), times.dayLength());
    }

    @Test
    @DisplayName("Tromso in December: polar night")
    void tromsoPolarNight() {
        SunTimes times = SolarCalculator.compute(
                new GeoPoint(69.6492, 18.9553), LocalDate.of(2026, 12, 21), TROMSO);

        assertEquals(DayState.POLAR_NIGHT, times.state());
        assertTrue(times.sunriseTime().isEmpty());
        assertTrue(times.sunsetTime().isEmpty());
        assertEquals(Duration.ZERO, times.dayLength());
    }

    @ParameterizedTest(name = "lat {0}, lon {1}, {2} is {3}")
    @CsvSource({
            "90.0,   0.0, 2026-06-21, POLAR_DAY",
            "90.0,   0.0, 2026-12-21, POLAR_NIGHT",
            "-90.0,  0.0, 2026-12-21, POLAR_DAY",
            "-90.0,  0.0, 2026-06-21, POLAR_NIGHT",
            "78.2232, 15.6469, 2026-06-21, POLAR_DAY",
            "78.2232, 15.6469, 2026-12-21, POLAR_NIGHT"
    })
    @DisplayName("Poles and Svalbard follow the same rule")
    void extremeLatitudes(double latitude, double longitude, String date, DayState expected) {
        SunTimes times = SolarCalculator.compute(
                new GeoPoint(latitude, longitude), LocalDate.parse(date), ZoneId.of("UTC"));

        assertEquals(expected, times.state());
    }

    @Test
    @DisplayName("Solar noon is reported even when the Sun does not rise")
    void solarNoonIsAlwaysAvailable() {
        SunTimes times = SolarCalculator.compute(
                new GeoPoint(69.6492, 18.9553), LocalDate.of(2026, 12, 21), TROMSO);

        assertNotNull(times.solarNoon());
        assertEquals(LocalDate.of(2026, 12, 21), times.solarNoon().toLocalDate());
    }
}
