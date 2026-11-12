package com.failurelab.service_b.client;

import com.failurelab.service_b.error.DownstreamException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.SocketTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ServiceAClient {

    private static final Logger log = LoggerFactory.getLogger(ServiceAClient.class);
    private static final String TARGET = "service-a";

    private final RestClient restClient;

    ServiceAClient(RestClient.Builder builder, DownstreamProperties properties) {
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());
        this.restClient = builder
                .baseUrl(properties.url())
                .requestFactory(ClientHttpRequestFactoryBuilder.simple().build(settings))
                .build();
    }

    public JsonNode cycle(int depth) {
        return get("/cycle?depth={depth}", depth);
    }

    private JsonNode get(String uri, Object... variables) {
        try {
            return restClient.get().uri(uri, variables).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Call to {} {} failed", TARGET, uri, e);
            throw isTimeout(e) ? DownstreamException.timeout(TARGET, e) : DownstreamException.error(TARGET, e);
        }
    }

    private static boolean isTimeout(Exception e) {
        return NestedExceptionUtils.getMostSpecificCause(e) instanceof SocketTimeoutException;
    }
}
