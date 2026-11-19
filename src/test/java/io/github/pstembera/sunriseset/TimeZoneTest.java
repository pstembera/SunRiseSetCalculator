package io.github.pstembera.sunriseset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The time zone must change only the representation of a result, never the
 * underlying instant.
 */
class TimeZoneTest {

    private static final GeoPoint PRAGUE = new GeoPoint(50.0755, 14.4378);
    private static final LocalDate SOLSTICE = LocalDate.of(2026, 6, 21);

    @Test
    @DisplayName("The same location and date give the same instant in every zone")
    void sameInstantAcrossZones() {
        SunTimes utc = SolarCalculator.compute(PRAGUE, SOLSTICE, ZoneId.of("UTC"));
        SunTimes prague = SolarCalculator.compute(PRAGUE, SOLSTICE, ZoneId.of("Europe/Prague"));
        SunTimes tokyo = SolarCalculator.compute(PRAGUE, SOLSTICE, ZoneId.of("Asia/Tokyo"));

        Instant expected = utc.sunrise().toInstant();
        assertEquals(expected, prague.sunrise().toInstant());
        assertEquals(expected, tokyo.sunrise().toInstant());
    }

    @Test
    @DisplayName("Local wall clock time follows the zone offset")
    void localTimeFollowsOffset() {
        SunTimes utc = SolarCalculator.compute(PRAGUE, SOLSTICE, ZoneId.of("UTC"));
        SunTimes prague = SolarCalculator.compute(PRAGUE, SOLSTICE, ZoneId.of("Europe/Prague"));

        // Same instant, but the wall clock in Prague is two hours ahead in June.
        assertEquals(utc.sunrise().toInstant(), prague.sunrise().toInstant());
        assertEquals(utc.sunrise().toLocalTime().plus(Duration.ofHours(2)), prague.sunrise().toLocalTime());
        assertEquals("Europe/Prague", prague.zone().getId());
    }

    @Test
    @DisplayName("Summer time is applied in June and standard time in December")
    void daylightSavingTime() {
        SunTimes summer = SolarCalculator.compute(PRAGUE, LocalDate.of(2026, 6, 21), ZoneId.of("Europe/Prague"));
        SunTimes winter = SolarCalculator.compute(PRAGUE, LocalDate.of(2026, 12, 21), ZoneId.of("Europe/Prague"));

        assertTrue(summer.sunrise().getOffset().getTotalSeconds() > winter.sunrise().getOffset().getTotalSeconds());
        assertEquals(2 * 3600, summer.sunrise().getOffset().getTotalSeconds());
        assertEquals(1 * 3600, winter.sunrise().getOffset().getTotalSeconds());
    }

    @Test
    @DisplayName("The result carries the requested zone and date")
    void resultCarriesRequestedContext() {
        ZoneId zone = ZoneId.of("Pacific/Auckland");
        SunTimes times = SolarCalculator.compute(PRAGUE, SOLSTICE, zone);

        assertEquals(zone, times.zone());
        assertEquals(SOLSTICE, times.date());
    }
}
