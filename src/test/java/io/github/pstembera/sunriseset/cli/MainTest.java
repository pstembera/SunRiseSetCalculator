package io.github.pstembera.sunriseset.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the command line without spawning a JVM.
 */
class MainTest {

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);

    @Test
    @DisplayName("--help prints usage and succeeds")
    void help() {
        assertEquals(Main.EXIT_OK, run("--help"));
        assertTrue(outText().contains("Usage: sun_rise_set"));
        assertTrue(outText().contains("--tz"));
    }

    @Test
    @DisplayName("Missing coordinates fail with the usage exit code")
    void missingCoordinates() {
        assertEquals(Main.EXIT_USAGE, run());
        assertTrue(errText().contains("Both --lat and --lon are required"));
    }

    @Test
    @DisplayName("Missing option value fails")
    void missingOptionValue() {
        assertEquals(Main.EXIT_USAGE, run("--lat"));
        assertTrue(errText().contains("Missing value for --lat"));
    }

    @Test
    @DisplayName("Out of range coordinates fail")
    void coordinatesOutOfRange() {
        assertEquals(Main.EXIT_USAGE, run("--lat", "100", "--lon", "14"));
        assertTrue(errText().contains("Latitude must be"));
    }

    @Test
    @DisplayName("Non numeric coordinates fail")
    void nonNumericCoordinates() {
        assertEquals(Main.EXIT_USAGE, run("--lat", "north", "--lon", "14"));
        assertTrue(errText().contains("Invalid --lat value"));
    }

    @Test
    @DisplayName("Malformed date fails")
    void malformedDate() {
        assertEquals(Main.EXIT_USAGE, run("--lat", "50", "--lon", "14", "--date", "21.6.2026"));
        assertTrue(errText().contains("Invalid --date value"));
    }

    @Test
    @DisplayName("Unknown time zone fails")
    void unknownTimeZone() {
        assertEquals(Main.EXIT_USAGE, run("--lat", "50", "--lon", "14", "--tz", "Mars/Olympus"));
        assertTrue(errText().contains("Unknown time zone"));
    }

    @Test
    @DisplayName("Unknown argument fails")
    void unknownArgument() {
        assertEquals(Main.EXIT_USAGE, run("--lat", "50", "--lon", "14", "--colour", "red"));
        assertTrue(errText().contains("Unknown argument"));
    }

    @Test
    @DisplayName("A normal day prints sunrise, sunset and day length")
    void normalDay() {
        int exit = run("--lat", "50.0755", "--lon", "14.4378", "--date", "2026-06-21", "--tz", "Europe/Prague");

        assertEquals(Main.EXIT_OK, exit);
        assertTrue(outText().contains("Sunrise:   2026-06-21T04:5"));
        assertTrue(outText().contains("Sunset:"));
        assertTrue(outText().contains("Day length: 16h"));
    }

    @Test
    @DisplayName("A polar day is reported instead of sunrise and sunset")
    void polarDay() {
        int exit = run("--lat", "69.6492", "--lon", "18.9553", "--date", "2026-06-21", "--tz", "Europe/Oslo");

        assertEquals(Main.EXIT_OK, exit);
        assertTrue(outText().contains("polar day"));
        assertTrue(!outText().contains("Day length"));
    }

    @Test
    @DisplayName("A polar night is reported instead of sunrise and sunset")
    void polarNight() {
        int exit = run("--lat", "69.6492", "--lon", "18.9553", "--date", "2026-12-21", "--tz", "Europe/Oslo");

        assertEquals(Main.EXIT_OK, exit);
        assertTrue(outText().contains("polar night"));
    }

    @Test
    @DisplayName("Durations are formatted for humans")
    void durationFormatting() {
        assertEquals("0h 00m 00s", Main.formatDuration(Duration.ZERO));
        assertEquals("1h 02m 03s", Main.formatDuration(Duration.ofSeconds(3723)));
        assertEquals("16h 23m 12s", Main.formatDuration(Duration.ofSeconds(16 * 3600 + 23 * 60 + 12)));
    }

    private int run(String... args) {
        outBytes.reset();
        errBytes.reset();
        return Main.run(args, out, err);
    }

    private String outText() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String errText() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }
}
