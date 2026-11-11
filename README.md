# PASHXXX

Android TV / Google TV and smartphone app built with Kotlin and Jetpack Compose.

## Current build: 0.3.0 (versionCode 6)

- Multilingual first-launch screen: Russian, German, English and Ukrainian.
- Dark TV-oriented home screen with four provider shortcuts and D-pad focus styling.
- Persistent preferences: language, card layout, accent colour and preferred playback handoff.
- Search opens the selected provider's own website search; there is no combined cross-provider catalogue.
- Embedded WebView browser with TV remote cursor support and Back navigation.
- External player handoff is attempted only when a provider navigates to a direct HTTP(S) media URL and the selected player is installed.

## Current limitations and safety

This is currently a provider-browser app, **not a unified video catalogue**. It does not create video cards, posters, thumbnails, trailers, category feeds or global search results. Those features require provider APIs or data sources that explicitly permit this use.

The app does not bypass DRM, authentication, paywalls, advertising, age checks, region restrictions, anti-bot controls or provider terms. Provider pages and embedded players remain subject to their own restrictions.

The in-app 18+ prompt is only a user self-declaration. It is **not identity verification or legally sufficient age assurance**. Public distribution must wait until age-assurance, privacy, jurisdictional and provider-terms requirements have been independently reviewed.

TV remote cursor interaction is a compatibility technique, not a guarantee that every site or embedded player will accept synthetic pointer input. Protected or cross-origin frames may not respond. Physical Android TV / Google TV and smartphone testing is still required; CI builds do not prove device compatibility.

## Build

Open **Actions** and select **Build PASHXXX APK** on branch `feature/tv-webview-mvp`. Download the artifact named `PASHXXX-v0.3.0-debug` after a successful workflow.

- Application ID: `com.smart222tech.pasha18tv`
- Version: `0.3.0` (versionCode 6)
- Minimum Android: API 23
- Target Android: API 35
- JDK: 17
