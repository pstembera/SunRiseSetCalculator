package io.github.pstembera.sunriseset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Coordinates are validated on construction so that the calculator never has to
 * deal with impossible positions.
 */
class GeoPointTest {

    @Test
    @DisplayName("Valid coordinates are accepted")
    void validCoordinates() {
        GeoPoint point = assertDoesNotThrow(() -> new GeoPoint(50.0755, 14.4378));

        assertEquals(50.0755, point.latitude());
        assertEquals(14.4378, point.longitude());
    }

    @ParameterizedTest(name = "latitude {0} is rejected")
    @ValueSource(doubles = {-90.0001, 90.0001, -180.0, 180.0, Double.NaN, Double.POSITIVE_INFINITY})
    @DisplayName("Invalid latitudes are rejected")
    void invalidLatitude(double latitude) {
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(latitude, 0.0));
    }

    @ParameterizedTest(name = "longitude {0} is rejected")
    @ValueSource(doubles = {-180.0001, 180.0001, Double.NaN, Double.NEGATIVE_INFINITY})
    @DisplayName("Invalid longitudes are rejected")
    void invalidLongitude(double longitude) {
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(0.0, longitude));
    }

    @Test
    @DisplayName("Boundary values are valid")
    void boundaryValues() {
        assertDoesNotThrow(() -> new GeoPoint(90.0, 180.0));
        assertDoesNotThrow(() -> new GeoPoint(-90.0, -180.0));
    }

    @Test
    @DisplayName("format uses a locale independent decimal point")
    void formatIsLocaleIndependent() {
        assertEquals("50.07550, 14.43780", new GeoPoint(50.0755, 14.4378).format());
    }
}
