# API Flow for Android

API Flow is a compact, mobile-first HTTP API client for Android. It is designed for testing APIs while away from a desktop workstation.

## Capabilities

- GET, POST, PUT, PATCH, DELETE, HEAD, and OPTIONS requests
- URL query parameters, request headers, raw request bodies, and response inspection
- Response status, latency, payload size, scrollable body viewer, and a 1 MiB safety cap
- Local request collections and request history
- `{{base_url}}` environment variable
- Light, dark, and system appearance
- No accounts, advertising, analytics, or developer-operated backend

## Local development

Requirements: JDK 17 and Android SDK Platform 36 / Build Tools 36.0.0. Point Gradle to the SDK with `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or a local (untracked) `local.properties` file.

```bash
./gradlew :app:assembleDebug
```

or:

```bash
./scripts/build-apk.sh
```

The installable development APK is written to `dist/api-flow-debug.apk`. The debug application ID is `io.github.jeffin_v.apiflow.debug`, so it can coexist with a release build.

## Google Play release

1. Decide on and reserve the permanent `applicationId` in `app/build.gradle` before the first upload.
2. Replace the publisher/contact placeholders in [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md), host it at a public HTTPS URL, and add the final URL in the Play Console and app release materials.
3. Generate and back up a private upload key. The passwords must be strong and must never enter version control:

```bash
export API_FLOW_STORE_PASSWORD='use-a-password-manager-generated-value'
export API_FLOW_KEY_PASSWORD='use-a-different-password-manager-generated-value'
export API_FLOW_KEY_DNAME='CN=Your Publisher, O=Your Company, C=IN'
./scripts/create-upload-key.sh
```

4. Build a signed Android App Bundle:

```bash
./scripts/build-release-bundle.sh
```

The release AAB is written to `dist/api-flow-release.aab`. Enrol the app in Play App Signing before upload. Increase `versionCode` for every Play release.

## F-Droid

The upstream Fastlane metadata and every tagged release are kept in this repository. API Flow uses F-Droid's reproducible-build flow: F-Droid independently rebuilds the tagged source and publishes the developer-signed APK only when it verifies as identical apart from its signature. The public source and privacy policy are hosted at [github.com/jeffin-v/api_flow](https://github.com/jeffin-v/api_flow).

## Security notes

Saved requests can contain credentials. API Flow uses Android app-private storage and disables device backup, but it is not a dedicated secret manager. Prefer HTTPS and do not persist long-lived production credentials unless the device security model is acceptable for the data.
