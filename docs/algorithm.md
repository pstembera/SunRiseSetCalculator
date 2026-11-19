# Algorithm

This document explains how `sun_rise_set` computes sunrise and sunset, why the
constants are what they are, and where the remaining error comes from.

## 1. Model

The implementation follows the **NOAA Solar Calculator** equations, which are a
Fourier fit of the Sun's position over one year. They are accurate enough for
civil use and simple enough to keep the library dependency free.

Everything is driven by a single argument, the **fractional year**:

```
gamma = 2 * PI / daysInYear * (dayOfYear + (hour - 12) / 24)
```

`dayOfYear` is 1-based, `daysInYear` is 365 or 366, and `hour` is the UTC hour at
which the ephemeris is evaluated. The series are fitted against the position of
the Sun in the middle of the day, so the day of year is used directly.

> **Why not `dayOfYear - 1`?** That variant shifts the ephemeris by half a day,
> which moves the declination by up to 0.4° near the equinoxes (the declination
> changes by about 0.4° per day). At 64° latitude that is worth three to five
> minutes of sunrise time. The test suite would catch it.

## 2. Equation of time and declination

```
eqtime = 229.18 * (0.000075
                 + 0.001868 cos(gamma)
                 - 0.032077 sin(gamma)
                 - 0.014615 cos(2 gamma)
                 - 0.040849 sin(2 gamma))          [minutes]

decl   = 0.006918
       - 0.399912 cos(gamma) + 0.070257 sin(gamma)
       - 0.006758 cos(2 gamma) + 0.000907 sin(2 gamma)
       - 0.002697 cos(3 gamma) + 0.00148 sin(3 gamma)   [radians]
```

`eqtime` is the difference between apparent and mean solar time; it is why solar
noon is not exactly at 12:00. `decl` is the angle between the Sun and the
celestial equator.

## 3. Hour angle

Sunrise and sunset happen when the centre of the Sun is at the official zenith:

```
zenith = 90.833°
```

That number is not arbitrary:

| Component | Value |
| --- | --- |
| 90° | geometric horizon |
| + 34′ | mean atmospheric refraction at the horizon |
| + 16′ | apparent solar radius |
| = 90° 50′ | **90.833°** |

The hour angle follows from the spherical triangle Sun–zenith–pole:

```
cos(ha) = cos(zenith) / (cos(lat) cos(decl)) - tan(lat) tan(decl)
```

If `cos(ha) > 1` the Sun never rises (**polar night**), if `cos(ha) < -1` it never
sets (**polar day**). Both cases are returned as `DayState` instead of an
exception, because they are normal results, not errors.

## 4. Times

Solar noon in minutes after UTC midnight:

```
solarNoon = 720 - 4 * longitude - eqtime
```

The factor 4 comes from the Earth rotating 1° per 4 minutes of time. Sunrise and
sunset are symmetric around solar noon:

```
sunrise = solarNoon - 4 * ha_degrees
sunset  = solarNoon + 4 * ha_degrees
```

The whole calculation runs **twice**. The first pass evaluates the ephemeris at
12:00, the second pass uses the solar noon computed by the first pass. The second
pass is what keeps the error around one minute instead of several.

## 5. Accuracy

The model error, not the implementation error, dominates:

| Source | Size |
| --- | --- |
| Fourier fit of the declination | ≈ 0.03° |
| Fourier fit of the equation of time | ≈ 0.3 min |
| Constant refraction instead of a pressure/temperature model | ≈ 0.1° |
| Spherical Earth, no observer elevation | small |

At low and mid latitudes one degree of declination is worth a few minutes of
time, so the errors stay under two minutes. Above 60° latitude the Sun crosses
the horizon at a shallow angle and the same declination error is amplified; the
tests therefore allow three minutes there.

If you need better than that, use a full ephemeris (for example the NREL Solar
Position Algorithm) and a refraction model that takes pressure and temperature
into account.

## 6. References

- NOAA Global Monitoring Laboratory, *Solar Calculator*
  <https://gml.noaa.gov/grad/solcalc/>
- United States Naval Observatory, *Astronomical Applications API*
  <https://aa.usno.navy.mil/data/api>
- sunrise-sunset.org, public API used as a second reference
  <https://sunrise-sunset.org/api>
- J. Meeus, *Astronomical Algorithms*, 2nd edition, for the reference
  declination and equation of time used to validate the series.
