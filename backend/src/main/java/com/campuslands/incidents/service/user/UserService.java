package com.campuslands.incidents.service.user;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuslands.incidents.dto.user.UserRequest;
import com.campuslands.incidents.dto.user.UserResponse;
import com.campuslands.incidents.entity.User;
import com.campuslands.incidents.exception.ConflictException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.IncidentRepository;
import com.campuslands.incidents.repository.UserRepository;

/**
 * User CRUD (RF01: every incident has a responsible user). Business rules:
 * <ul>
 *   <li>Emails are stored in lower case and are unique (409 on duplicates).</li>
 *   <li>A user responsible for incidents cannot be deleted (409), so the history is kept.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    static final String DUPLICATE_EMAIL = "Ya existe un usuario con ese correo";
    static final String HAS_INCIDENTS = "El usuario es responsable de incidentes y no se puede eliminar";

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;

    public UserService(UserRepository userRepository, IncidentRepository incidentRepository) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("name")).stream()
                .map(UserService::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(getUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(DUPLICATE_EMAIL);
        }
        User saved = userRepository.save(new User(request.name().trim(), email, request.area().trim()));
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = getUser(id);
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException(DUPLICATE_EMAIL);
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setArea(request.area().trim());
        return toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getUser(id);
        if (incidentRepository.existsByUserId(id)) {
            throw new ConflictException(HAS_INCIDENTS);
        }
        userRepository.delete(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /** Emails are case-insensitive, so they are stored in a single form. */
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getArea());
    }
}
