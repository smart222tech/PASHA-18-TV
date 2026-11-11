# PASHXXX

Native Android TV / Google TV and Android app built with Kotlin, Jetpack Compose and AndroidX Media3.

## Native catalogue foundation: 0.4.0 (versionCode 7)

- Replaces the provider-WebView landing experience with a native Compose catalogue layout.
- Adds native video poster-card, scene-details and Media3 playback screens.
- Includes an explicit empty-catalogue state until an authorized feed is configured.
- Catalogue entries require title, synopsis, poster image URL, category, duration and a permitted playback URL.
- No sample or fabricated videos/streams are presented as real content.

## Important integration requirement

The catalogue repository is intentionally empty. A real catalogue needs a provider/API/feed that explicitly permits displaying its posters and descriptions and playing its media URLs. The app will not scrape sites, bypass DRM, authentication, paywalls, age checks, region restrictions, anti-bot controls or provider terms.

The in-app age prompt in previous builds was only self-declaration and is not legally sufficient age assurance. Public distribution requires independent review of age assurance, privacy, jurisdictional requirements and provider terms.

## Build

GitHub Actions workflow: **Build PASHXXX APK**.

- Application ID: `com.smart222tech.pasha18tv`
- Version: `0.4.0` (versionCode 7)
- Minimum Android: API 23
- Target Android: API 35
- JDK: 17
