package com.failurelab.service_b.cycle;

import com.fasterxml.jackson.databind.JsonNode;

record CycleResponse(String service, int depth, boolean limitReached, JsonNode downstream) {
}
