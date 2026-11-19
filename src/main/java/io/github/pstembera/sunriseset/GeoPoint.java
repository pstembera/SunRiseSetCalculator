package io.github.pstembera.sunriseset;

/**
 * A geographic position on Earth in decimal degrees.
 *
 * <p>Latitude is positive to the north and negative to the south, longitude is
 * positive to the east and negative to the west. Both values are validated on
 * construction, so an instance always describes a real position.</p>
 *
 * @param latitude  latitude in degrees, from {@code -90.0} to {@code 90.0}
 * @param longitude longitude in degrees, from {@code -180.0} to {@code 180.0}
 */
public record GeoPoint(double latitude, double longitude) {

    /**
     * Creates a validated geographic point.
     *
     * @throws IllegalArgumentException if a coordinate is not finite or out of range
     */
    public GeoPoint {
        if (!Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    "Latitude must be a finite value between -90 and 90 degrees, got: " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    "Longitude must be a finite value between -180 and 180 degrees, got: " + longitude);
        }
    }

    /**
     * Returns the position formatted as {@code "lat, lon"} with five decimal
     * places, which is roughly one metre of precision.
     *
     * @return a human readable representation of the coordinates
     */
    public String format() {
        return String.format(java.util.Locale.ROOT, "%.5f, %.5f", latitude, longitude);
    }
}
