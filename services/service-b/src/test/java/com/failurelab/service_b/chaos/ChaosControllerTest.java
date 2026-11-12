package com.failurelab.service_b.chaos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChaosController.class)
@Import(ChaosState.class)
class ChaosControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ChaosState state;

    @Test
    void shouldActivateLatencyWithDefaultDelayWhenBodyIsMissing() throws Exception {
        mvc.perform(post("/chaos/latency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("service-b"))
                .andExpect(jsonPath("$.active.latency").value(true))
                .andExpect(jsonPath("$.delayMs").value(2000));
    }

    @Test
    void shouldActivateLatencyWithRequestedDelay() throws Exception {
        mvc.perform(post("/chaos/latency").contentType(MediaType.APPLICATION_JSON).content("{\"delayMs\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delayMs").value(500));
    }

    @Test
    void shouldRejectDelayBelowOne() throws Exception {
        mvc.perform(post("/chaos/latency").contentType(MediaType.APPLICATION_JSON).content("{\"delayMs\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldRejectDelayAboveTenSeconds() throws Exception {
        mvc.perform(post("/chaos/latency").contentType(MediaType.APPLICATION_JSON).content("{\"delayMs\":10001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldRejectMalformedBody() throws Exception {
        mvc.perform(post("/chaos/latency").contentType(MediaType.APPLICATION_JSON).content("{\"delayMs\":\"slow\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldReturnNotFoundForScenarioThatServiceBDoesNotOwn() throws Exception {
        mvc.perform(post("/chaos/cycle"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("UNKNOWN_SCENARIO"));
    }

    @Test
    void shouldTurnLatencyOffOnReset() throws Exception {
        state.activateLatency(1000);

        mvc.perform(delete("/chaos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active.latency").value(false))
                .andExpect(jsonPath("$.delayMs").value(0));
    }

    @Test
    void shouldReportCurrentState() throws Exception {
        state.activateLatency(750);

        mvc.perform(get("/chaos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active.latency").value(true))
                .andExpect(jsonPath("$.delayMs").value(750));
    }
}
