package com.failurelab.service_a.chaos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.failurelab.service_a.chaos.ResourceExhaustionJob.ExhaustionSettings;
import com.failurelab.service_a.error.ScenarioAlreadyRunningException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ResourceExhaustionJobTest {

    private static final ExhaustionSettings GENTLE = new ExhaustionSettings(1, 5000, 1);

    private final ResourceExhaustionJob job = new ResourceExhaustionJob();

    @AfterEach
    void stopJob() {
        job.stop();
    }

    @Test
    void shouldBeRunningAfterStart() {
        job.start(GENTLE);

        assertThat(job.isRunning()).isTrue();
    }

    @Test
    void shouldRefuseSecondStartWhileRunning() {
        job.start(GENTLE);

        assertThatThrownBy(() -> job.start(GENTLE)).isInstanceOf(ScenarioAlreadyRunningException.class);
    }

    @Test
    void shouldStopAndAllowNewStart() {
        job.start(GENTLE);
        job.stop();

        assertThat(job.isRunning()).isFalse();
        job.start(GENTLE);
        assertThat(job.isRunning()).isTrue();
    }

    @Test
    void shouldIgnoreStopWhenNotRunning() {
        job.stop();

        assertThat(job.isRunning()).isFalse();
    }
}
