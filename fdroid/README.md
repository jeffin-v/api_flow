# F-Droid submission package

This directory contains the completed metadata needed for an F-Droid submission. F-Droid will build, sign, and publish the app itself.

1. Publish this complete source repository at a public Git URL and tag the release as `v1.0.0`.
2. Copy `com.apiflow.mobile.yml` into a fork of the [`fdroiddata`](https://gitlab.com/fdroid/fdroiddata) repository as `metadata/com.apiflow.mobile.yml`.
4. Run F-Droid's lint/build checks and open a merge request. F-Droid builds the source itself; do not attach this project's APK or AAB.

The app uses only Android platform APIs and includes no advertising, tracking, accounts, or non-free runtime dependency. Keep future releases within those constraints or declare the appropriate F-Droid anti-feature.
