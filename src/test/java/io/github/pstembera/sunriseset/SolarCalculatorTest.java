package io.github.pstembera.sunriseset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares computed times with reference values published by the United States
 * Naval Observatory (USNO) and with the sunrise-sunset.org service.
 *
 * <p>The NOAA model is accurate to roughly one minute, so the assertions allow a
 * small tolerance. Above 60 degrees of latitude the Sun crosses the horizon at a
 * shallow angle and the same model error translates into more minutes, so the
 * tolerance is relaxed there.</p>
 */
class SolarCalculatorTest {

    private static final Duration TOLERANCE = Duration.ofMinutes(2);
    private static final Duration HIGH_LATITUDE_TOLERANCE = Duration.ofMinutes(3);

    @Test
    @DisplayName("Prague, summer solstice")
    void pragueSummerSolstice() {
        SunTimes times = compute(50.0755, 14.4378, "2026-06-21", "Europe/Prague");

        assertEquals(DayState.NORMAL, times.state());
        assertClose("sunrise", "2026-06-21T02:53:00Z", times.sunrise(), TOLERANCE);
        assertClose("sunset", "2026-06-21T19:16:00Z", times.sunset(), TOLERANCE);
        assertClose("solar noon", "2026-06-21T11:04:00Z", times.solarNoon(), TOLERANCE);

        // Reference day length is 16 h 23 min.
        assertTrue(times.dayLength().compareTo(Duration.ofMinutes(16 * 60 + 20)) > 0);
        assertTrue(times.dayLength().compareTo(Duration.ofMinutes(16 * 60 + 26)) < 0);
    }

    @Test
    @DisplayName("Prague, winter solstice uses CET")
    void pragueWinterSolstice() {
        SunTimes times = compute(50.0755, 14.4378, "2026-12-21", "Europe/Prague");

        assertClose("sunrise", "2026-12-21T06:58:00Z", times.sunrise(), TOLERANCE);
        assertClose("sunset", "2026-12-21T15:02:00Z", times.sunset(), TOLERANCE);
        // December is standard time in Central Europe, so both events use +01:00.
        assertEquals(1, times.sunrise().getOffset().getTotalSeconds() / 3600);
        assertEquals(1, times.sunset().getOffset().getTotalSeconds() / 3600);
    }

    @Test
    @DisplayName("Sydney, southern hemisphere across the date line")
    void sydneySummerSolstice() {
        SunTimes times = compute(-33.8688, 151.2093, "2026-12-21", "Australia/Sydney");

        // The local sunrise belongs to the 21st, the UTC instant still to the 20th.
        assertEquals(LocalDate.of(2026, 12, 21), times.sunrise().toLocalDate());
        assertClose("sunrise", "2026-12-20T18:41:00Z", times.sunrise(), TOLERANCE);
        assertClose("sunset", "2026-12-21T09:05:00Z", times.sunset(), TOLERANCE);
    }

    @Test
    @DisplayName("Quito, equinox on the equator")
    void quitoEquinox() {
        SunTimes times = compute(-0.1807, -78.4678, "2026-03-20", "America/Guayaquil");

        assertClose("sunrise", "2026-03-20T11:18:00Z", times.sunrise(), TOLERANCE);
        assertClose("sunset", "2026-03-20T23:24:00Z", times.sunset(), TOLERANCE);

        // On the equator the day is slightly longer than 12 hours because of refraction.
        assertTrue(times.dayLength().compareTo(Duration.ofMinutes(12 * 60)) > 0);
        assertTrue(times.dayLength().compareTo(Duration.ofMinutes(12 * 60 + 12)) < 0);
    }

    @Test
    @DisplayName("Reykjavik, high latitude where the model error grows")
    void reykjavikSeptember() {
        SunTimes times = compute(64.1466, -21.9426, "2026-09-01", "Atlantic/Reykjavik");

        assertClose("sunrise", "2026-09-01T06:10:00Z", times.sunrise(), HIGH_LATITUDE_TOLERANCE);
        assertClose("sunset", "2026-09-01T20:43:00Z", times.sunset(), HIGH_LATITUDE_TOLERANCE);
    }

    @Test
    @DisplayName("Solar noon is always between sunrise and sunset")
    void solarNoonIsBetweenSunriseAndSunset() {
        SunTimes times = compute(50.0755, 14.4378, "2026-06-21", "Europe/Prague");

        assertTrue(times.solarNoon().isAfter(times.sunrise()));
        assertTrue(times.solarNoon().isBefore(times.sunset()));
    }

    @Test
    @DisplayName("Day length equals the difference between sunset and sunrise")
    void dayLengthMatchesSunsetMinusSunrise() {
        SunTimes times = compute(-33.8688, 151.2093, "2026-12-21", "Australia/Sydney");

        assertEquals(Duration.between(times.sunrise(), times.sunset()), times.dayLength());
    }

    private static SunTimes compute(double lat, double lon, String date, String zone) {
        return SolarCalculator.compute(
                new GeoPoint(lat, lon),
                LocalDate.parse(date),
                ZoneId.of(zone));
    }

    private static void assertClose(String label, String expectedIso, ZonedDateTime actual, Duration tolerance) {
        assertNotNull(actual, label + " should be present");
        Instant expected = Instant.parse(expectedIso);
        Duration difference = Duration.between(expected, actual.toInstant()).abs();
        assertTrue(difference.compareTo(tolerance) <= 0,
                () -> label + " expected " + expected + " but was " + actual.toInstant()
                        + ", off by " + difference);
    }
}
