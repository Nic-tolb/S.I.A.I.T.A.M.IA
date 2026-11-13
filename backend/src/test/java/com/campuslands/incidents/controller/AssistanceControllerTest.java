package com.campuslands.incidents.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.campuslands.incidents.dto.assistance.AssistanceAnswer;
import com.campuslands.incidents.dto.assistance.AssistanceResponse;
import com.campuslands.incidents.dto.assistance.AssistanceSource;
import com.campuslands.incidents.entity.enums.QueryStatus;
import com.campuslands.incidents.exception.GlobalExceptionHandler;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.service.assistance.AiQueryService;

class AssistanceControllerTest {

    private final AiQueryService service = mock(AiQueryService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AssistanceController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsTheAssistantAnswer() throws Exception {
        when(service.assist(7L, "No hay internet")).thenReturn(new AssistanceResponse(42L, 7L, QueryStatus.ANSWERED,
                new AssistanceAnswer("Renovar la IP", List.of("Abrir cmd", "ipconfig /renew")),
                List.of(new AssistanceSource(1L, "Sin acceso a internet", 0.64)), null, Instant.now()));

        mvc.perform(post("/api/incidents/7/assistance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"No hay internet\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").value(42))
                .andExpect(jsonPath("$.status").value("ANSWERED"))
                .andExpect(jsonPath("$.answer.steps.length()").value(2))
                .andExpect(jsonPath("$.sources[0].articleId").value(1))
                .andExpect(jsonPath("$.sources[0].similarity").value(0.64));
    }

    @Test
    void blankDescriptionIsRejectedBeforeCallingTheAssistant() throws Exception {
        mvc.perform(post("/api/incidents/7/assistance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.description").value("La descripción es obligatoria"));

        verify(service, never()).assist(anyLong(), any());
    }

    @Test
    void unknownIncidentIs404() throws Exception {
        when(service.assist(99L, "Sin internet")).thenThrow(new ResourceNotFoundException("Incident", 99L));

        mvc.perform(post("/api/incidents/99/assistance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Sin internet\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Incident with id 99 does not exist"));
    }
}
