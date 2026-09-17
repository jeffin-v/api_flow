package io.github.jeffin_v.apiflow;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CollectionStorageTest {
    @Test
    public void savesEnteredUrlInsteadOfExpandedUrlWithQueryRows() {
        String enteredUrl = "https://httpbin.org/post";
        assertEquals(enteredUrl, CollectionStorage.urlForSave(enteredUrl));
    }

    @Test
    public void preservesManuallyEnteredQueryString() {
        String enteredUrl = "https://example.test/search?fixed=1";
        assertEquals(enteredUrl, CollectionStorage.urlForSave(enteredUrl));
    }

    @Test
    public void migratesLegacyUrlThatContainsSavedParameters() {
        String legacyUrl = "https://httpbin.org/post?search=a%20b%26c";

        assertEquals(
                "https://httpbin.org/post",
                CollectionStorage.urlForLoad(legacyUrl, "search=a%20b%26c", 1));
    }

    @Test
    public void migrationPreservesManualQueryBeforeSavedParameters() {
        String legacyUrl = "https://example.test/search?fixed=1&editable=2";

        assertEquals(
                "https://example.test/search?fixed=1",
                CollectionStorage.urlForLoad(legacyUrl, "editable=2", 1));
    }

    @Test
    public void currentFormatLeavesEnteredUrlUntouched() {
        String enteredUrl = "https://example.test/search?fixed=1";

        assertEquals(
                enteredUrl,
                CollectionStorage.urlForLoad(enteredUrl, "editable=2", CollectionStorage.ENTERED_URL_FORMAT));
    }
}
