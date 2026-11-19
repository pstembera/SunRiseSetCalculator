package io.github.pstembera.sunriseset;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Computes sunrise, sunset and solar noon from the NOAA solar equations.
 *
 * <p>The implementation is a direct port of the NOAA Solar Calculator
 * spreadsheets: it derives the fractional year, the equation of time and the
 * solar declination from the day of the year, then solves the hour angle at
 * which the Sun reaches the official zenith of {@value #OFFICIAL_ZENITH_DEGREES}
 * degrees. The calculation is repeated once with the resulting solar noon to
 * remove the error caused by evaluating the ephemeris at 12:00 instead of at
 * the real solar noon.</p>
 *
 * <p>Expected accuracy is about one to two minutes, which is the accuracy of
 * the model itself rather than of this implementation.</p>
 *
 * <p>The class is stateless and thread safe.</p>
 */
public final class SolarCalculator {

    /**
     * Official zenith used for sunrise and sunset: 90 degrees plus 50 arc
     * minutes of atmospheric refraction plus 16 arc minutes of the solar
     * radius.
     */
    private static final double OFFICIAL_ZENITH_DEGREES = 90.833;

    private static final double SECONDS_PER_MINUTE = 60.0;
    private static final double MINUTES_PER_HALF_DAY = 720.0;
    private static final double MINUTES_PER_DEGREE = 4.0;

    private SolarCalculator() {
    }

    /**
     * Computes the solar events for a location and a local date.
     *
     * @param location geographic position, must not be {@code null}
     * @param date     local date to compute for, must not be {@code null}
     * @param zone     time zone used for the returned instants, must not be {@code null}
     * @return the computed times; sunrise and sunset are absent during polar day/night
     */
    public static SunTimes compute(GeoPoint location, LocalDate date, ZoneId zone) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");

        double latitude = Math.toRadians(location.latitude());
        double longitude = location.longitude();
        int dayOfYear = date.getDayOfYear();
        int daysInYear = date.lengthOfYear();

        // First guess: solar noon happens at 12:00 local solar time.
        double solarNoonMinutes = MINUTES_PER_HALF_DAY - MINUTES_PER_DEGREE * longitude;
        double cosHourAngle = 0.0;

        for (int pass = 0; pass < 2; pass++) {
            double gamma = fractionalYear(dayOfYear, solarNoonMinutes / 60.0, daysInYear);
            double equationOfTime = equationOfTime(gamma);
            double declination = solarDeclination(gamma);

            solarNoonMinutes = MINUTES_PER_HALF_DAY - MINUTES_PER_DEGREE * longitude - equationOfTime;

            cosHourAngle = Math.cos(Math.toRadians(OFFICIAL_ZENITH_DEGREES))
                    / (Math.cos(latitude) * Math.cos(declination))
                    - Math.tan(latitude) * Math.tan(declination);

            if (cosHourAngle > 1.0 || cosHourAngle < -1.0) {
                // The Sun never reaches (or never leaves) the horizon, a second
                // pass cannot change that because the declination barely moves.
                break;
            }
        }

        Instant utcMidnight = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        ZonedDateTime solarNoon = toZoned(utcMidnight, solarNoonMinutes, zone);

        if (cosHourAngle > 1.0) {
            return new SunTimes(date, zone, DayState.POLAR_NIGHT, null, null, solarNoon, Duration.ZERO);
        }
        if (cosHourAngle < -1.0) {
            return new SunTimes(date, zone, DayState.POLAR_DAY, null, null, solarNoon, Duration.ofDays(1));
        }

        double hourAngleDegrees = Math.toDegrees(Math.acos(cosHourAngle));
        double sunriseMinutes = solarNoonMinutes - MINUTES_PER_DEGREE * hourAngleDegrees;
        double sunsetMinutes = solarNoonMinutes + MINUTES_PER_DEGREE * hourAngleDegrees;

        ZonedDateTime sunrise = toZoned(utcMidnight, sunriseMinutes, zone);
        ZonedDateTime sunset = toZoned(utcMidnight, sunsetMinutes, zone);

        return new SunTimes(
                date,
                zone,
                DayState.NORMAL,
                sunrise,
                sunset,
                solarNoon,
                Duration.between(sunrise, sunset));
    }

    private static ZonedDateTime toZoned(Instant utcMidnight, double minutesFromUtcMidnight, ZoneId zone) {
        long seconds = Math.round(minutesFromUtcMidnight * SECONDS_PER_MINUTE);
        return utcMidnight.plusSeconds(seconds).atZone(zone);
    }

    /**
     * Fractional year in radians, the single input of both NOAA approximations.
     *
     * <p>The series are fitted against the position of the Sun at the middle of
     * the day, so the day of year is used directly and the hour only moves the
     * argument away from noon. Using {@code dayOfYear - 1} here shifts the
     * ephemeris by half a day, which is worth up to 0.4 degrees of declination
     * near the equinoxes and several minutes of sunrise time at high
     * latitudes.</p>
     */
    private static double fractionalYear(int dayOfYear, double hour, int daysInYear) {
        return 2.0 * Math.PI / daysInYear * (dayOfYear + (hour - 12.0) / 24.0);
    }

    /**
     * Equation of time in minutes, the difference between apparent and mean
     * solar time.
     */
    private static double equationOfTime(double gamma) {
        return 229.18 * (0.000075
                + 0.001868 * Math.cos(gamma)
                - 0.032077 * Math.sin(gamma)
                - 0.014615 * Math.cos(2 * gamma)
                - 0.040849 * Math.sin(2 * gamma));
    }

    /**
     * Solar declination in radians, the angle between the Sun and the equator.
     */
    private static double solarDeclination(double gamma) {
        return 0.006918
                - 0.399912 * Math.cos(gamma)
                + 0.070257 * Math.sin(gamma)
                - 0.006758 * Math.cos(2 * gamma)
                + 0.000907 * Math.sin(2 * gamma)
                - 0.002697 * Math.cos(3 * gamma)
                + 0.00148 * Math.sin(3 * gamma);
    }
}
