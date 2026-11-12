package com.failurelab.service_b.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.failurelab.service_b.chaos.ChaosState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.BDDMockito.given;

@WebMvcTest(ProcessController.class)
class ProcessControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChaosState state;

    @Test
    void shouldAnswerOkWithoutDelayWhenLatencyIsOff() throws Exception {
        long start = System.nanoTime();

        mvc.perform(get("/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("service-b"))
                .andExpect(jsonPath("$.status").value("OK"));

        assertThat(elapsedMs(start)).isLessThan(150);
    }

    @Test
    void shouldDelayAnswerByConfiguredLatency() throws Exception {
        given(state.isLatencyActive()).willReturn(true);
        given(state.latencyMs()).willReturn(200);
        long start = System.nanoTime();

        mvc.perform(get("/process")).andExpect(status().isOk());

        assertThat(elapsedMs(start)).isGreaterThanOrEqualTo(200);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
