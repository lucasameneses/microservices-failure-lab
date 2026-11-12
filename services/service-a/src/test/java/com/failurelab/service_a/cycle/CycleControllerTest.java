package com.failurelab.service_a.cycle;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.failurelab.service_a.client.ServiceBClient;
import com.failurelab.service_a.error.DownstreamException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CycleController.class)
class CycleControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ServiceBClient peer;

    @Test
    void shouldStopWithoutCallingPeerWhenMaxDepthIsReached() throws Exception {
        mvc.perform(get("/cycle").param("depth", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("service-a"))
                .andExpect(jsonPath("$.depth").value(10))
                .andExpect(jsonPath("$.limitReached").value(true))
                .andExpect(jsonPath("$.downstream").isEmpty());

        verify(peer, never()).cycle(anyInt());
    }

    @Test
    void shouldCallPeerWithNextDepthBelowLimit() throws Exception {
        given(peer.cycle(4)).willReturn(mapper.readTree("{\"service\":\"service-b\",\"depth\":4}"));

        mvc.perform(get("/cycle").param("depth", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depth").value(3))
                .andExpect(jsonPath("$.limitReached").value(false))
                .andExpect(jsonPath("$.downstream.service").value("service-b"))
                .andExpect(jsonPath("$.downstream.depth").value(4));
    }

    @Test
    void shouldStartAtDepthZeroWhenParameterIsMissing() throws Exception {
        given(peer.cycle(1)).willReturn(mapper.readTree("{}"));

        mvc.perform(get("/cycle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depth").value(0));

        verify(peer).cycle(1);
    }

    @Test
    void shouldRejectNegativeDepth() throws Exception {
        mvc.perform(get("/cycle").param("depth", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldRejectNonNumericDepth() throws Exception {
        mvc.perform(get("/cycle").param("depth", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldReturnBadGatewayWhenPeerFails() throws Exception {
        given(peer.cycle(1)).willThrow(DownstreamException.error("service-b", new RuntimeException("Connection refused")));

        mvc.perform(get("/cycle"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("DOWNSTREAM_ERROR"));
    }
}
