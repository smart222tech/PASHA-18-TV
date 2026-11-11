# PASHXXX

Native Android TV / Google TV video application built with Kotlin, Jetpack Compose and AndroidX Media3.

## Current native catalogue foundation: 0.5.0 (versionCode 8)

- PASHXXX launcher identity and application ID: `com.smart222tech.pashxxx`.
- Native Compose catalogue layout with video-card, details and Media3 playback foundations.
- Android TV / Google TV launcher entry point and landscape-first layout.
- Explicit empty-catalogue state until an authorized metadata and playback feed is configured.
- No fake sample videos or fabricated stream URLs.

## Important integration requirement

A real catalogue needs a provider/API/feed that explicitly permits displaying its posters and descriptions and playing its media URLs. The current catalogue repository is intentionally empty. A browser/cursor mode and provider integration must be added and tested as a separate feature; they are not claimed as implemented in this build.

The app will not scrape sites, bypass DRM, authentication, paywalls, age checks, region restrictions, anti-bot controls or provider terms. Only lawful, authorized sources and user-provided media URLs should be integrated.

## Build

GitHub Actions workflow: **Build PASHXXX APK**.

- Application ID: `com.smart222tech.pashxxx`
- Kotlin namespace (kept stable for Activity compatibility): `com.smart222tech.pasha18tv`
- Version: `0.5.0` (versionCode 8)
- Minimum Android: API 23
- Target Android: API 35
- JDK: 17
