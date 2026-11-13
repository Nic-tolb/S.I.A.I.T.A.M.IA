package com.campuslands.incidents.service.incident;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.campuslands.incidents.dto.incident.IncidentFilter;
import com.campuslands.incidents.entity.enums.IncidentPriority;

class IncidentSpecificationsTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void dayStartsAtMidnightInBogotaWhichIsFiveInTheMorningUtc() {
        assertThat(IncidentSpecifications.startOfDay(LocalDate.of(2026, 10, 1), BOGOTA))
                .isEqualTo(Instant.parse("2026-10-01T05:00:00Z"));
    }

    @Test
    void toDateIncludesTheWholeDayUntilTheNextMidnight() {
        assertThat(IncidentSpecifications.startOfNextDay(LocalDate.of(2026, 10, 31), BOGOTA))
                .isEqualTo(Instant.parse("2026-11-01T05:00:00Z"));
    }

    @Test
    void incidentLateAtNightInBogotaBelongsToThatLocalDay() {
        // 2026-10-31 23:30 in Bogotá is already 2026-11-01 in UTC, but must count as October 31
        Instant lateNight = Instant.parse("2026-11-01T04:30:00Z");
        LocalDate day = LocalDate.of(2026, 10, 31);

        assertThat(lateNight)
                .isAfterOrEqualTo(IncidentSpecifications.startOfDay(day, BOGOTA))
                .isBefore(IncidentSpecifications.startOfNextDay(day, BOGOTA));
    }

    @Test
    void buildsSpecificationWithAndWithoutFilters() {
        var none = new IncidentFilter(null, null, null, null, null);
        var all = new IncidentFilter(2L, IncidentPriority.P1, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(IncidentSpecifications.fromFilter(none, BOGOTA)).isNotNull();
        assertThat(IncidentSpecifications.fromFilter(all, BOGOTA)).isNotNull();
    }
}
