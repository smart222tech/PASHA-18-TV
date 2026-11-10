# PASHA 18+ TV

Android TV / Google TV app built with Kotlin and Jetpack Compose.

## Current build: 0.2.0

- First-launch language selection: Russian, German, English and Ukrainian.
- TV-friendly home screen with left navigation, source tiles and D-pad focus states.
- Persistent settings: language, 3 / 9 / 18-card layout or list, accent colour, internal browser / VLC / MX Player preference.
- Search screen routes the user to a selected provider. A true cross-provider video search is not yet available because provider catalog adapters have not been integrated.
- Embedded WebView browser for the four configured providers.
- External player handoff is attempted only for direct HTTP(S) media URLs. Provider-controlled embedded players may not expose such URLs.
- GitHub Actions builds a debug APK artifact.

## Important current limitations

This version is a multilingual TV interface and provider browser, **not yet a unified video catalogue**. It does not currently extract video thumbnails, create hover trailers, synchronize categories, or perform global search across provider catalogs. Those require provider-specific, permitted catalog integrations; a static tile must not be presented as a real video item.

The app does not bypass DRM, authentication, access controls, provider advertising, age/region restrictions, or site terms. If a provider disallows embedded playback, use its permitted viewing method.

The in-app 18+ confirmation is not legally sufficient age verification. Do not distribute publicly until age-assurance, privacy, jurisdictional and provider-terms requirements have been reviewed.

## Build

Open **Actions** and select **Build PASHA 18+ APK** on branch `feature/tv-webview-mvp`. Download the artifact named `PASHA-18-TV-v0.2.0-debug`.

- Application ID: `com.smart222tech.pasha18tv`
- Version: `0.2.0` (versionCode 3)
- Minimum Android: API 23
- Target Android: API 35
- JDK: 17
