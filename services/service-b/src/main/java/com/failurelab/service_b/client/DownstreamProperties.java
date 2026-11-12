package com.failurelab.service_b.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("lab.downstream")
public record DownstreamProperties(String url, Duration connectTimeout, Duration readTimeout) {
}
