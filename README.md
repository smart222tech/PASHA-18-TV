# PASHA 18+ TV

Android TV / Google TV starter app built with Kotlin and Jetpack Compose.

## Current MVP

- Large, D-pad-focusable source tiles for Pornhub, xVideos, xHamster and XNXX.
- Simple 18+ confirmation screen.
- Opens each provider inside Android WebView.
- Back button navigates WebView history first, then returns to the source grid.
- Debug APK is built by GitHub Actions and uploaded as an artifact.

## Important scope and limitations

This MVP is a **site launcher and embedded browser**, not a stream extractor. It does not scrape private APIs, bypass DRM, defeat access controls, remove provider advertising, or circumvent age/region restrictions. Sites control their own catalog, playback, login, ads and availability. If a provider disallows embedded playback, use its permitted viewing method.

The 18+ confirmation is a basic in-app gate, not legally sufficient age verification. Do not distribute publicly until age-assurance, privacy, jurisdictional and provider-terms requirements have been reviewed.

## Build

GitHub Actions workflow: `.github/workflows/build-apk.yml`

1. Open the repository's **Actions** tab.
2. Open **Build PASHA 18+ APK** for branch `feature/tv-webview-mvp`.
3. Download the `PASHA-18-TV-debug` artifact.
4. Extract `app-debug.apk` and install it on a test device.

Local build (JDK 17 and Gradle 8.9):

```bash
gradle assembleDebug
```

## Package

- Application ID: `com.smart222tech.pasha18tv`
- Version: `0.1.0` (versionCode 1)
- Minimum Android: API 23
- Target Android: API 35
