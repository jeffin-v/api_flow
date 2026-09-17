package io.github.jeffin_v.apiflow;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AppIdentityTest {
    @Test
    public void userAgentUsesCurrentBuildVersion() {
        assertEquals("API-Flow-Android/" + BuildConfig.VERSION_NAME, AppIdentity.userAgent());
    }

    @Test
    public void privacySummaryUsesCurrentBuildVersion() {
        assertEquals("Effective version: " + BuildConfig.VERSION_NAME, AppIdentity.privacyVersionLabel());
    }
}
