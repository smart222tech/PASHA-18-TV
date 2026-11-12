# PASHXXX

Native Android TV / Google TV video application built with Kotlin, Jetpack Compose and AndroidX Media3.

## Current native catalogue foundation: 0.5.2 (versionCode 10)

- PASHXXX launcher identity and application ID: `com.smart222tech.pashxxx`.
- Native Compose catalogue layout with selectable Home / Catalogue / Favorites sections, video-card, details and Media3 playback foundations.
- D-pad-focusable sidebar items with an animated, high-contrast focus outline.
- Android TV / Google TV launcher entry point and landscape-first layout.
- Explicit empty-catalogue state until an authorized metadata and playback feed is configured.
- No fake sample videos or fabricated stream URLs.

## Important integration requirement

A real catalogue needs a provider/API/feed that explicitly permits displaying its posters and descriptions and playing its media URLs. The current catalogue repository is intentionally empty. A separate Android TV browser activity is present with a D-pad air-mouse cursor, HTTP(S) URL bar and HTML5 custom-view fullscreen handling. The remote Play key now attempts playback and fullscreen for a visible HTML5 video; sites that block scripted fullscreen or use non-HTML5 players may not support this. Compatibility still requires device testing.

The app will not scrape sites, bypass DRM, authentication, paywalls, age checks, region restrictions, anti-bot controls or provider terms. Only lawful, authorized sources and user-provided media URLs should be integrated.

## Build

GitHub Actions workflow: **Build PASHXXX APK** (artifact: `PASHXXX-v0.5.2-debug`).

- Application ID: `com.smart222tech.pashxxx`
- Kotlin namespace (kept stable for Activity compatibility): `com.smart222tech.pasha18tv`
- Version: `0.5.2` (versionCode 9)
- Minimum Android: API 23
- Target Android: API 35
- JDK: 17
