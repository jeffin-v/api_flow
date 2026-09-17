package io.github.jeffin_v.apiflow;

/** Version-derived identifiers shown to users and sent to API endpoints. */
final class AppIdentity {
    private AppIdentity() { }

    static String userAgent() {
        return "API-Flow-Android/" + BuildConfig.VERSION_NAME;
    }

    static String privacyVersionLabel() {
        return "Effective version: " + BuildConfig.VERSION_NAME;
    }
}
