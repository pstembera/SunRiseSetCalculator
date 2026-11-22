# SunRiseSetCalculator

[![CI](https://github.com/pstembera/SunRiseSetCalculator/actions/workflows/ci.yml/badge.svg)](https://github.com/pstembera/SunRiseSetCalculator/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/java-21-orange.svg)](https://adoptium.net/)

A small, dependency-free Java library and command line tool that computes
**sunrise, sunset, solar noon and day length** for any GPS coordinate and any
date.

The implementation follows the NOAA solar equations and is accurate to roughly
one minute at moderate latitudes. Polar day and polar night are handled
explicitly instead of producing nonsense values.

## Features

- Zero runtime dependencies, pure JDK 21
- Works with any `ZoneId`, including daylight saving transitions
- Correct behaviour above the polar circles (`POLAR_DAY`, `POLAR_NIGHT`)
- Handles the date line, the southern hemisphere and both solstices
- Immutable records, thread safe, no static mutable state
- 45 unit tests checked against USNO and sunrise-sunset.org reference values

## Quick start

```powershell
git clone https://github.com/pstembera/SunRiseSetCalculator.git
cd SunRiseSetCalculator
.\mvnw.cmd verify          # build and run the tests (use ./mvnw on Linux/macOS)
```

Run the CLI:

```powershell
java -jar target/SunRiseSetCalculator.jar --lat 50.0755 --lon 14.4378 --date 2026-06-21 --tz Europe/Prague
```

```
Date:      2026-06-21
Zone:      Europe/Prague
Sunrise:   2026-06-21T04:52:11+02:00[Europe/Prague]
Sunset:    2026-06-21T21:15:23+02:00[Europe/Prague]
Solar noon: 2026-06-21T13:03:47+02:00[Europe/Prague]
Day length: 16h 23m 12s
```

## Command line

```
Usage: SunRiseSetCalculator --lat <degrees> --lon <degrees> [--date <yyyy-MM-dd>] [--tz <zone>]

Options:
  --lat <degrees>    Latitude, -90 to 90 (required)
  --lon <degrees>    Longitude, -180 to 180 (required)
  --date <date>      Local date in ISO format, defaults to today
  --tz <zone>        Time zone id such as Europe/Prague, defaults to UTC
  -h, --help         Show this help
```

Exit codes: `0` success, `2` invalid arguments or coordinates.

Without `--tz` the output is in UTC on purpose: a deterministic default is
easier to script against than the time zone of the machine.

### Polar day and polar night

```powershell
java -jar target/SunRiseSetCalculator.jar --lat 69.6492 --lon 18.9553 --date 2026-06-21 --tz Europe/Oslo
```

```
Date:      2026-06-21
Zone:      Europe/Oslo
Sunrise:   - (polar day, the Sun does not set)
Solar noon: 2026-06-21T12:45:43+02:00[Europe/Oslo]
```

## Library usage

```java
import io.github.pstembera.sunriseset.*;
import java.time.*;

SunTimes times = SolarCalculator.compute(
        new GeoPoint(50.0755, 14.4378),   // Prague
        LocalDate.of(2026, 6, 21),
        ZoneId.of("Europe/Prague"));

times.sunrise();          // 2026-06-21T04:52:11+02:00[Europe/Prague]
times.sunset();           // 2026-06-21T21:15:23+02:00[Europe/Prague]
times.solarNoon();        // 2026-06-21T13:03:47+02:00[Europe/Prague]
times.dayLength();        // PT16H23M12S
times.state();            // DayState.NORMAL

// During polar day or polar night the sunrise/sunset are absent:
times.sunriseTime();      // Optional<ZonedDateTime>
```

Coordinates are validated in the `GeoPoint` constructor, so invalid input fails
fast with `IllegalArgumentException`.

## Accuracy

| Latitude | Expected error |
| --- | --- |
| 0° – 60° | within 2 minutes |
| 60° – 66° | within 3 minutes |

The error comes from the NOAA approximation of the solar ephemeris, not from the
implementation: the Sun crosses the horizon at a shallow angle near the polar
circles, so the same declination error turns into more minutes. The test suite
enforces these bounds against USNO reference data.

## How it works

The short version:

1. The fractional year and the NOAA Fourier series give the equation of time and
   the solar declination.
2. The hour angle at which the Sun reaches the official zenith of 90.833°
   (refraction + solar radius) follows from spherical trigonometry.
3. Sunrise and sunset are symmetric around solar noon, which is derived from the
   longitude and the equation of time.
4. The calculation is repeated once with the resulting solar noon to remove the
   error of evaluating the ephemeris at 12:00.

The full derivation, the formulas and the reasoning behind the constants are in
[docs/algorithm.md](docs/algorithm.md).

## Project layout

```
src/main/java/io/github/pstembera/sunriseset/   library
src/main/java/io/github/pstembera/sunriseset/cli/Main.java   command line
src/test/java/                                  JUnit 5 tests
docs/algorithm.md                               algorithm documentation
examples/                                       ready to run examples
```

## Examples

See [examples/README.md](examples/README.md) for ready to run commands for
Prague, Sydney, Tromsø and Quito.

## Building

Requires JDK 21. Maven is not required, the wrapper downloads it.

```powershell
.\mvnw.cmd verify                # compile, test, package
.\mvnw.cmd javadoc:javadoc       # generate target/site/apidocs
```

## License

[MIT](LICENSE) © 2026 pstembera

First review done.

