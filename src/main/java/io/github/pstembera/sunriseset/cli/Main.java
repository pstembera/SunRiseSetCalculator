package io.github.pstembera.sunriseset.cli;

import io.github.pstembera.sunriseset.DayState;
import io.github.pstembera.sunriseset.GeoPoint;
import io.github.pstembera.sunriseset.SolarCalculator;
import io.github.pstembera.sunriseset.SunTimes;

import java.io.PrintStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRulesException;
import java.util.Locale;

/**
 * Command line entry point.
 *
 * <pre>
 * java -jar SunRiseSetCalculator.jar --lat 50.0755 --lon 14.4378 --date 2026-06-21 --tz Europe/Prague
 * </pre>
 */
public final class Main {

    /** Exit code for a successful run. */
    public static final int EXIT_OK = 0;

    /** Exit code for invalid arguments or input values. */
    public static final int EXIT_USAGE = 2;

    private static final String USAGE = """
            Usage: SunRiseSetCalculator --lat <degrees> --lon <degrees> [--date <yyyy-MM-dd>] [--tz <zone>]

            Options:
              --lat <degrees>    Latitude, -90 to 90 (required)
              --lon <degrees>    Longitude, -180 to 180 (required)
              --date <date>      Local date in ISO format, defaults to today
              --tz <zone>        Time zone id such as Europe/Prague, defaults to UTC
              -h, --help         Show this help

            Examples:
              SunRiseSetCalculator --lat 50.0755 --lon 14.4378 --tz Europe/Prague
              SunRiseSetCalculator --lat -33.8688 --lon 151.2093 --date 2026-12-21 --tz Australia/Sydney
            """;

    private Main() {
    }

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Runs the application without terminating the JVM, which makes the
     * command line testable.
     *
     * @param args command line arguments
     * @param out  stream for the result
     * @param err  stream for error messages
     * @return the process exit code
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Double latitude = null;
        Double longitude = null;
        LocalDate date = null;
        String zoneId = "UTC";

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> {
                    out.print(USAGE);
                    return EXIT_OK;
                }
                case "--lat" -> {
                    String value = valueOf(args, ++i, "--lat", err);
                    if (value == null) {
                        return EXIT_USAGE;
                    }
                    latitude = parseDouble(value, "--lat", err);
                    if (latitude == null) {
                        return EXIT_USAGE;
                    }
                }
                case "--lon" -> {
                    String value = valueOf(args, ++i, "--lon", err);
                    if (value == null) {
                        return EXIT_USAGE;
                    }
                    longitude = parseDouble(value, "--lon", err);
                    if (longitude == null) {
                        return EXIT_USAGE;
                    }
                }
                case "--date" -> {
                    String value = valueOf(args, ++i, "--date", err);
                    if (value == null) {
                        return EXIT_USAGE;
                    }
                    try {
                        date = LocalDate.parse(value);
                    } catch (DateTimeParseException e) {
                        err.println("Invalid --date value, expected yyyy-MM-dd: " + value);
                        return EXIT_USAGE;
                    }
                }
                case "--tz" -> {
                    String value = valueOf(args, ++i, "--tz", err);
                    if (value == null) {
                        return EXIT_USAGE;
                    }
                    zoneId = value;
                }
                default -> {
                    err.println("Unknown argument: " + arg);
                    err.println();
                    err.print(USAGE);
                    return EXIT_USAGE;
                }
            }
        }

        if (latitude == null || longitude == null) {
            err.println("Both --lat and --lon are required.");
            err.println();
            err.print(USAGE);
            return EXIT_USAGE;
        }

        ZoneId zone;
        try {
            zone = ZoneId.of(zoneId);
        } catch (ZoneRulesException | DateTimeParseException e) {
            err.println("Unknown time zone: " + zoneId);
            return EXIT_USAGE;
        }

        GeoPoint location;
        try {
            location = new GeoPoint(latitude, longitude);
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
            return EXIT_USAGE;
        }

        LocalDate effectiveDate = date != null ? date : LocalDate.now(zone);
        SunTimes times = SolarCalculator.compute(location, effectiveDate, zone);
        printResult(times, out);
        return EXIT_OK;
    }

    private static void printResult(SunTimes times, PrintStream out) {
        out.printf(Locale.ROOT, "Date:      %s%n", times.date());
        out.printf(Locale.ROOT, "Zone:      %s%n", times.zone());

        switch (times.state()) {
            case NORMAL -> {
                out.printf(Locale.ROOT, "Sunrise:   %s%n", times.sunrise());
                out.printf(Locale.ROOT, "Sunset:    %s%n", times.sunset());
            }
            case POLAR_DAY -> out.println("Sunrise:   - (polar day, the Sun does not set)");
            case POLAR_NIGHT -> out.println("Sunrise:   - (polar night, the Sun does not rise)");
            default -> throw new IllegalStateException("Unhandled state: " + times.state());
        }

        out.printf(Locale.ROOT, "Solar noon: %s%n", times.solarNoon());

        if (times.state() == DayState.NORMAL) {
            out.printf(Locale.ROOT, "Day length: %s%n", formatDuration(times.dayLength()));
        }
    }

    /**
     * Formats a duration as {@code 16h 17m 51s}, which reads better than the
     * ISO-8601 form produced by {@link Duration#toString()}.
     *
     * @param duration duration to format
     * @return a compact human readable representation
     */
    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format(Locale.ROOT, "%dh %02dm %02ds", hours, minutes, secs);
    }

    private static String valueOf(String[] args, int index, String option, PrintStream err) {
        if (index >= args.length) {
            err.println("Missing value for " + option);
            return null;
        }
        return args[index];
    }

    private static Double parseDouble(String value, String option, PrintStream err) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                err.println("Invalid " + option + " value: " + value);
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            err.println("Invalid " + option + " value: " + value);
            return null;
        }
    }

    /**
     * Renders a zoned time, mostly a convenience for tests and callers that do
     * not want to depend on {@link ZonedDateTime#toString()}.
     *
     * @param time time to render
     * @return the ISO-8601 representation
     */
    public static String format(ZonedDateTime time) {
        return time.toString();
    }
}
