package com.campuslands.incidents.service.category;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campuslands.incidents.dto.category.CategoryRequest;
import com.campuslands.incidents.dto.category.CategoryResponse;
import com.campuslands.incidents.entity.Category;
import com.campuslands.incidents.exception.ConflictException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.CategoryRepository;
import com.campuslands.incidents.repository.IncidentRepository;

/**
 * Category CRUD (RF01). Business rules:
 * <ul>
 *   <li>Names are unique ignoring case (409 on duplicates).</li>
 *   <li>A category with incidents cannot be deleted (409), so the analytics history is kept.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    static final String DUPLICATE_NAME = "Ya existe una categoría con ese nombre";
    static final String HAS_INCIDENTS = "La categoría tiene incidentes asociados y no se puede eliminar";

    private final CategoryRepository categoryRepository;
    private final IncidentRepository incidentRepository;

    public CategoryService(CategoryRepository categoryRepository, IncidentRepository incidentRepository) {
        this.categoryRepository = categoryRepository;
        this.incidentRepository = incidentRepository;
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryService::toResponse)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException(DUPLICATE_NAME);
        }
        Category saved = categoryRepository.save(new Category(name, normalize(request.description())));
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException(DUPLICATE_NAME);
        }
        category.setName(name);
        category.setDescription(normalize(request.description()));
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (incidentRepository.existsByCategoryId(id)) {
            throw new ConflictException(HAS_INCIDENTS);
        }
        categoryRepository.delete(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    /** Optional description: trimmed, and stored as null when blank. */
    private static String normalize(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
