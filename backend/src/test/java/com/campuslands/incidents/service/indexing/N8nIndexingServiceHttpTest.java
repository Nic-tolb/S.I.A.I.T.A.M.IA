package com.campuslands.incidents.service.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.campuslands.incidents.client.N8nClient;
import com.campuslands.incidents.config.N8nProperties;
import com.campuslands.incidents.config.RestClientConfig;
import com.campuslands.incidents.dto.indexing.ArticleIndexPayload;
import com.campuslands.incidents.dto.indexing.IndexingResult;

/**
 * Checks the HTTP contract with n8n flow 1 (n8n/README.md): JSON body, secret header and response parsing.
 */
class N8nIndexingServiceHttpTest {

    private static final String BASE_URL = "http://n8n:5678";
    private static final String PATH = "/webhook/index-articles";

    private MockRestServiceServer server;
    private IndexingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(RestClientConfig.WEBHOOK_SECRET_HEADER, "test-secret");
        server = MockRestServiceServer.bindTo(builder).build();
        N8nProperties props = new N8nProperties(BASE_URL, "test-secret", PATH, "/webhook/rag-assistant",
                Duration.ofSeconds(5), Duration.ofSeconds(60));
        service = new N8nIndexingService(new N8nClient(builder.build()), props);
    }

    @Test
    void sendsArticlesInTheAgreedFormatAndParsesTheResponse() {
        server.expect(requestTo(BASE_URL + PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(RestClientConfig.WEBHOOK_SECRET_HEADER, "test-secret"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.articles.length()").value(1))
                .andExpect(jsonPath("$.articles[0].id").value(12))
                .andExpect(jsonPath("$.articles[0].title").value("Sin acceso a internet"))
                .andExpect(jsonPath("$.articles[0].symptoms").value("No cargan páginas"))
                .andExpect(jsonPath("$.articles[0].active").value(true))
                .andRespond(withSuccess("{\"indexed\":1,\"deleted\":0,\"errors\":[]}", MediaType.APPLICATION_JSON));

        IndexingResult result = service.syncArticle(new ArticleIndexPayload(12, "Sin acceso a internet",
                "El equipo no navega", "No cargan páginas", "IP inválida", "Renovar la IP", true));

        server.verify();
        assertThat(result).isEqualTo(new IndexingResult(1, 0, java.util.List.of()));
    }

    @Test
    void errorStatusFromN8nBecomesAFailedResult() {
        server.expect(requestTo(BASE_URL + PATH))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"indexed\":0,\"deleted\":0,\"errors\":[\"Upsert points failed\"]}"));

        IndexingResult result = service.removeArticle(12);

        server.verify();
        assertThat(result.successful()).isFalse();
        assertThat(result.errors()).singleElement().asString().contains("502");
    }
}
