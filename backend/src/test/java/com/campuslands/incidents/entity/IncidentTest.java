package com.campuslands.incidents.entity;

import com.campuslands.incidents.entity.enums.IncidentPriority;
import com.campuslands.incidents.entity.enums.IncidentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class IncidentTest {

    private Incident newIncident() {
        Category category = new Category("Conectividad", "Red e internet");
        User user = new User("Ana", "ana@example.com", "TI");
        return new Incident("Sin internet", "No hay conexión en el piso 3", category, user, IncidentPriority.P2);
    }

    @Test
    void newIncidentIsOpenAndNotResolved() {
        Incident incident = newIncident();

        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        assertNull(incident.getResolvedAt());
    }

    @Test
    void resolvingSetsResolvedAt() {
        Incident incident = newIncident();

        incident.changeStatus(IncidentStatus.RESOLVED);

        assertNotNull(incident.getResolvedAt());
    }

    @Test
    void closingAResolvedIncidentKeepsOriginalResolvedAt() {
        Incident incident = newIncident();
        incident.changeStatus(IncidentStatus.RESOLVED);
        Instant resolvedAt = incident.getResolvedAt();

        incident.changeStatus(IncidentStatus.CLOSED);

        assertEquals(resolvedAt, incident.getResolvedAt());
    }

    @Test
    void reopeningClearsResolvedAt() {
        Incident incident = newIncident();
        incident.changeStatus(IncidentStatus.RESOLVED);

        incident.changeStatus(IncidentStatus.IN_PROGRESS);

        assertNull(incident.getResolvedAt());
    }
}
