package io.github.pstembera.sunriseset;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a solar calculation: sunrise, sunset, solar noon and day length for
 * one location and one local date.
 *
 * <p>During polar day and polar night there is no sunrise or sunset, so
 * {@link #sunrise()} and {@link #sunset()} are {@code null}. Use
 * {@link #sunriseTime()} and {@link #sunsetTime()} to work with
 * {@link Optional} instead.</p>
 *
 * @param date      the local date the calculation was made for
 * @param zone      the time zone used for the returned instants
 * @param state     whether the Sun rises and sets on that day
 * @param sunrise   local sunrise, or {@code null} during polar day/night
 * @param sunset    local sunset, or {@code null} during polar day/night
 * @param solarNoon the moment the Sun reaches its highest point
 * @param dayLength how long the Sun is above the horizon
 */
public record SunTimes(
        LocalDate date,
        ZoneId zone,
        DayState state,
        ZonedDateTime sunrise,
        ZonedDateTime sunset,
        ZonedDateTime solarNoon,
        Duration dayLength) {

    /**
     * Validates the consistency of the result.
     *
     * @throws NullPointerException     if a non-nullable component is {@code null}
     * @throws IllegalArgumentException if the state does not match the presence of sunrise/sunset
     */
    public SunTimes {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(solarNoon, "solarNoon");
        Objects.requireNonNull(dayLength, "dayLength");

        boolean hasSunriseAndSunset = sunrise != null && sunset != null;
        if (state == DayState.NORMAL && !hasSunriseAndSunset) {
            throw new IllegalArgumentException("A normal day requires both sunrise and sunset");
        }
        if (state != DayState.NORMAL && (sunrise != null || sunset != null)) {
            throw new IllegalArgumentException("Polar day and polar night must not carry sunrise or sunset");
        }
    }

    /**
     * Returns the sunrise time if the Sun crosses the horizon on this day.
     *
     * @return the sunrise, or an empty optional during polar day/night
     */
    public Optional<ZonedDateTime> sunriseTime() {
        return Optional.ofNullable(sunrise);
    }

    /**
     * Returns the sunset time if the Sun crosses the horizon on this day.
     *
     * @return the sunset, or an empty optional during polar day/night
     */
    public Optional<ZonedDateTime> sunsetTime() {
        return Optional.ofNullable(sunset);
    }
}
