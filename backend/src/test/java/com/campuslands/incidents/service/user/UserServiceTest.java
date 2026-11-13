package com.campuslands.incidents.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.campuslands.incidents.dto.user.UserRequest;
import com.campuslands.incidents.dto.user.UserResponse;
import com.campuslands.incidents.entity.User;
import com.campuslands.incidents.exception.ConflictException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.UserRepository;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final UserService service = new UserService(userRepository, incidentRepository);

    /** Users get their id from the database, so tests set it by reflection. */
    private static User user(long id, String name, String email) {
        User user = new User(name, email, "Finanzas");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void findAllReturnsUsersSortedByName() {
        when(userRepository.findAll(Sort.by("name"))).thenReturn(List.of(
                user(2, "Andrés Ruiz", "andres.ruiz@empresa.com"),
                user(1, "Laura Gómez", "laura.gomez@empresa.com")));

        List<UserResponse> result = service.findAll();

        assertThat(result).extracting(UserResponse::name).containsExactly("Andrés Ruiz", "Laura Gómez");
    }

    @Test
    void findByIdThrowsNotFoundWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTrimsFieldsAndStoresEmailInLowerCase() {
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new UserRequest("  Laura Gómez ", "  Laura.Gomez@Empresa.COM ", " Finanzas "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Laura Gómez");
        assertThat(saved.getValue().getEmail()).isEqualTo("laura.gomez@empresa.com");
        assertThat(saved.getValue().getArea()).isEqualTo("Finanzas");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("laura.gomez@empresa.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new UserRequest("Laura", "Laura.Gomez@empresa.com", "TI")))
                .isInstanceOf(ConflictException.class)
                .hasMessage(UserService.DUPLICATE_EMAIL);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateChangesAllFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, "Laura", "laura@empresa.com")));

        UserResponse result = service.update(1L, new UserRequest("Laura Gómez", "LGomez@empresa.com", "Soporte"));

        assertThat(result).isEqualTo(new UserResponse(1L, "Laura Gómez", "lgomez@empresa.com", "Soporte"));
    }

    @Test
    void updateRejectsEmailUsedByAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, "Laura", "laura@empresa.com")));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("andres@empresa.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new UserRequest("Laura", "andres@empresa.com", "TI")))
                .isInstanceOf(ConflictException.class)
                .hasMessage(UserService.DUPLICATE_EMAIL);
    }

    @Test
    void deleteRejectsUserResponsibleForIncidents() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1, "Laura", "laura@empresa.com")));
        when(incidentRepository.existsByUserId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage(UserService.HAS_INCIDENTS);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesUserWithoutIncidents() {
        User existing = user(1, "Laura", "laura@empresa.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(incidentRepository.existsByUserId(1L)).thenReturn(false);

        service.delete(1L);

        verify(userRepository).delete(existing);
    }
}
