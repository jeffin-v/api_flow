package io.github.jeffin_v.apiflow;

/** Defines which URL representation is persisted with an editable saved request. */
final class CollectionStorage {
    static final int ENTERED_URL_FORMAT = 2;

    private CollectionStorage() { }

    static String urlForSave(String enteredUrl) {
        return enteredUrl == null ? "" : enteredUrl.trim();
    }

    static String urlForLoad(String storedUrl, String encodedSavedParams, int formatVersion) {
        String url = storedUrl == null ? "" : storedUrl;
        if (formatVersion >= ENTERED_URL_FORMAT || encodedSavedParams == null || encodedSavedParams.isEmpty()) {
            return url;
        }

        String queryOnlySuffix = "?" + encodedSavedParams;
        if (url.endsWith(queryOnlySuffix)) {
            return url.substring(0, url.length() - queryOnlySuffix.length());
        }

        String appendedSuffix = "&" + encodedSavedParams;
        if (url.endsWith(appendedSuffix)) {
            return url.substring(0, url.length() - appendedSuffix.length());
        }
        return url;
    }
}
