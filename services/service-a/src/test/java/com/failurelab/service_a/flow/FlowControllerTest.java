package com.failurelab.service_a.flow;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.failurelab.service_a.chaos.ChaosState;
import com.failurelab.service_a.client.ServiceBClient;
import com.failurelab.service_a.error.DownstreamException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FlowController.class)
class FlowControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ServiceBClient serviceB;

    @MockitoBean
    private ChaosState state;

    @Test
    void shouldNestServiceBProcessResponseInNormalFlow() throws Exception {
        given(serviceB.process()).willReturn(mapper.readTree("{\"service\":\"service-b\",\"status\":\"OK\"}"));

        mvc.perform(get("/flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("service-a"))
                .andExpect(jsonPath("$.downstream.status").value("OK"));

        verify(serviceB, never()).cycle(1);
    }

    @Test
    void shouldEnterCycleAtDepthOneWhenCycleIsActive() throws Exception {
        given(state.isCycleActive()).willReturn(true);
        given(serviceB.cycle(1)).willReturn(mapper.readTree("{\"service\":\"service-b\",\"depth\":1}"));

        mvc.perform(get("/flow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downstream.depth").value(1));

        verify(serviceB, never()).process();
    }

    @Test
    void shouldReturnBadGatewayWhenServiceBIsUnreachable() throws Exception {
        given(serviceB.process()).willThrow(DownstreamException.error("service-b", new RuntimeException("Connection refused")));

        mvc.perform(get("/flow"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("DOWNSTREAM_ERROR"));
    }

    @Test
    void shouldReturnGatewayTimeoutWhenServiceBIsTooSlow() throws Exception {
        given(serviceB.process()).willThrow(DownstreamException.timeout("service-b", new SocketTimeoutException("Read timed out")));

        mvc.perform(get("/flow"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("DOWNSTREAM_TIMEOUT"));
    }
}
