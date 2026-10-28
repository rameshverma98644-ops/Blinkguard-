# BlinkGuard Android

Kotlin Android project with a user-enabled screen-tint overlay and a screen-on reminder timer.

## Build an APK from a phone using GitHub Actions

1. Download and extract this ZIP.
2. Create a GitHub repository named `BlinkGuard-Android`.
3. Upload the *contents* of the extracted folder to the repository root. Keep `.github/workflows/build-apk.yml` at that exact path.
4. Open the repository's **Actions** tab and select **Build BlinkGuard APK**.
5. Wait for a green check, open that run, then download the `BlinkGuard-debug-APK` artifact.
6. Extract the downloaded artifact and install `app-debug.apk`. Android may ask to allow installs from your browser or file manager.

## Build configuration

- JDK 17
- Android Gradle Plugin 8.6.1
- Kotlin Gradle Plugin 2.0.20
- Gradle 8.7
- compileSdk 35, targetSdk 34, minSdk 24

## Known limitations

- The overlay tints/darkens pixels; it does not change hardware brightness or provide a true OS-level blue-light filter.
- Reminder countdown pauses when the screen is off, but it measures elapsed screen-on time, not actual foreground-app interaction. OEM battery controls can still stop background services.
- User must grant **Display over other apps** permission.
- GitHub Actions creates a debug APK. It is not a Play Store-signed release. A release needs a protected signing key and policy review.
- Android 14+ special-use foreground-service policy and Play Store review may affect distribution.
