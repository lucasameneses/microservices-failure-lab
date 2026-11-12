package com.failurelab.service_a.chaos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.failurelab.service_a.chaos.ResourceExhaustionJob.ExhaustionSettings;
import com.failurelab.service_a.error.ScenarioAlreadyRunningException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChaosController.class)
@Import(ChaosState.class)
class ChaosControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ChaosState state;

    @MockitoBean
    private ResourceExhaustionJob job;

    @Test
    void shouldActivateCycle() throws Exception {
        mvc.perform(post("/chaos/cycle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("service-a"))
                .andExpect(jsonPath("$.active.cycle").value(true));
    }

    @Test
    void shouldStartExhaustionWithDefaultSettingsWhenBodyIsMissing() throws Exception {
        mvc.perform(post("/chaos/resource-exhaustion")).andExpect(status().isOk());

        verify(job).start(new ExhaustionSettings(2, 500, 2));
    }

    @Test
    void shouldStartExhaustionWithRequestedSettings() throws Exception {
        mvc.perform(post("/chaos/resource-exhaustion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chunkMb\":8,\"intervalMs\":100,\"cpuThreads\":4}"))
                .andExpect(status().isOk());

        verify(job).start(new ExhaustionSettings(8, 100, 4));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"chunkMb\":0}", "{\"chunkMb\":65}",
            "{\"intervalMs\":49}", "{\"intervalMs\":5001}",
            "{\"cpuThreads\":0}", "{\"cpuThreads\":9}"})
    void shouldRejectExhaustionSettingsOutOfRange(String body) throws Exception {
        mvc.perform(post("/chaos/resource-exhaustion").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));

        verify(job, never()).start(any());
    }

    @Test
    void shouldReturnConflictWhenExhaustionIsAlreadyRunning() throws Exception {
        willThrow(new ScenarioAlreadyRunningException("resource-exhaustion")).given(job).start(any());

        mvc.perform(post("/chaos/resource-exhaustion"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SCENARIO_ALREADY_RUNNING"));
    }

    @Test
    void shouldReturnNotFoundForScenarioThatServiceADoesNotOwn() throws Exception {
        mvc.perform(post("/chaos/latency"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UNKNOWN_SCENARIO"));
    }

    @Test
    void shouldResetCycleAndStopExhaustion() throws Exception {
        state.activateCycle();
        given(job.isRunning()).willReturn(false);

        mvc.perform(delete("/chaos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active.cycle").value(false))
                .andExpect(jsonPath("$.active['resource-exhaustion']").value(false));

        verify(job).stop();
    }
}
