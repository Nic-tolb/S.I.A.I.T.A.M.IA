package com.campuslands.incidents.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.campuslands.incidents.dto.indexing.IndexingResult;
import com.campuslands.incidents.service.indexing.KnowledgeReindexService;

class KnowledgeIndexingControllerTest {

    private final KnowledgeReindexService service = mock(KnowledgeReindexService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new KnowledgeIndexingController(service)).build();

    @Test
    void returns200WithCountsWhenEverythingWasIndexed() throws Exception {
        when(service.reindexAll()).thenReturn(new IndexingResult(25, 0, List.of()));

        mvc.perform(post("/api/knowledge-articles/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(25))
                .andExpect(jsonPath("$.deleted").value(0))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void returns502WithTheErrorsWhenIndexingFailed() throws Exception {
        when(service.reindexAll()).thenReturn(IndexingResult.failed("AI service is unavailable or timed out"));

        mvc.perform(post("/api/knowledge-articles/reindex"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errors[0]").value("AI service is unavailable or timed out"));
    }
}
