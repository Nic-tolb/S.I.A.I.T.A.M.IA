package com.campuslands.incidents.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.campuslands.incidents.exception.AiServiceException;

/**
 * Single outbound gateway to n8n. Translates network failures, timeouts and error
 * responses into {@link AiServiceException} so the query ends up in ERROR status.
 */
@Component
public class N8nClient {

    private static final Logger log = LoggerFactory.getLogger(N8nClient.class);

    private final RestClient n8nRestClient;

    public N8nClient(RestClient n8nRestClient) {
        this.n8nRestClient = n8nRestClient;
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        try {
            return n8nRestClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientResponseException e) {
            log.error("n8n responded {} on {}: {}", e.getStatusCode().value(), path, e.getResponseBodyAsString());
            throw new AiServiceException("AI service responded with error " + e.getStatusCode().value(), e);
        } catch (ResourceAccessException e) {
            log.error("Could not reach n8n on {}: {}", path, e.getMessage());
            throw new AiServiceException("AI service is unavailable or timed out", e);
        }
    }
}
