package com.failurelab.service_a.cycle;

import com.fasterxml.jackson.databind.JsonNode;

record CycleResponse(String service, int depth, boolean limitReached, JsonNode downstream) {
}
