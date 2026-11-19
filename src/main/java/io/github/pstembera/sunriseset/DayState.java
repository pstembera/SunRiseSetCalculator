package io.github.pstembera.sunriseset;

/**
 * Describes whether the Sun crosses the horizon on a given day.
 *
 * <p>Inside the polar circles the Sun may stay above or below the horizon for
 * the whole day, in which case there is no sunrise and no sunset to compute.</p>
 */
public enum DayState {

    /** The Sun rises and sets; both times are available. */
    NORMAL,

    /** The Sun stays above the horizon for the whole day (midnight sun). */
    POLAR_DAY,

    /** The Sun stays below the horizon for the whole day (polar night). */
    POLAR_NIGHT
}
