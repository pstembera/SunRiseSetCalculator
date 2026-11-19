# Examples

Build the jar once, then run any of the examples below:

```powershell
.\mvnw.cmd -DskipTests package
```

## Prague, summer solstice

```powershell
.\examples\run.ps1 -Lat 50.0755 -Lon 14.4378 -Date 2026-06-21 -Tz Europe/Prague
```

```
Sunrise:   2026-06-21T04:52:11+02:00[Europe/Prague]
Sunset:    2026-06-21T21:15:23+02:00[Europe/Prague]
Day length: 16h 23m 12s
```

## Sydney, summer solstice in the southern hemisphere

```powershell
.\examples\run.ps1 -Lat -33.8688 -Lon 151.2093 -Date 2026-12-21 -Tz Australia/Sydney
```

```
Sunrise:   2026-12-21T05:41:03+11:00[Australia/Sydney]
Sunset:    2026-12-21T20:05:28+11:00[Australia/Sydney]
Day length: 14h 24m 25s
```

## Tromsø, midnight sun

```powershell
.\examples\run.ps1 -Lat 69.6492 -Lon 18.9553 -Date 2026-06-21 -Tz Europe/Oslo
```

```
Sunrise:   - (polar day, the Sun does not set)
```

## Quito, on the equator at the equinox

```powershell
.\examples\run.ps1 -Lat -0.1807 -Lon -78.4678 -Date 2026-03-20 -Tz America/Guayaquil
```

```
Sunrise:   2026-03-20T06:18:20-05:00[America/Guayaquil]
Sunset:    2026-03-20T18:25-05:00[America/Guayaquil]
Day length: 12h 06m 40s
```

## Linux and macOS

The same commands with `run.sh`:

```bash
./examples/run.sh --lat 50.0755 --lon 14.4378 --date 2026-06-21 --tz Europe/Prague
```
