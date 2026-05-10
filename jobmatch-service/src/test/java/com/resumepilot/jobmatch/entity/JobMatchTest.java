package com.resumepilot.jobmatch.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobMatchTest {

    @Test
    void onCreateInitializesMatchedAtAndBookmarkFlag() {
        JobMatch match = new JobMatch();
        match.setBookmarked(true);

        match.onCreate();

        assertThat(match.getMatchedAt()).isNotNull();
        assertThat(match.isBookmarked()).isFalse();
    }
}
