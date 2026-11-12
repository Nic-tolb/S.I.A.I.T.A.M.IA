package com.campuslands.incidents.service.category;

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

import com.campuslands.incidents.dto.category.CategoryRequest;
import com.campuslands.incidents.dto.category.CategoryResponse;
import com.campuslands.incidents.entity.Category;
import com.campuslands.incidents.exception.ConflictException;
import com.campuslands.incidents.exception.ResourceNotFoundException;
import com.campuslands.incidents.repository.CategoryRepository;
import com.campuslands.incidents.repository.IncidentRepository;

class CategoryServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final CategoryService service = new CategoryService(categoryRepository, incidentRepository);

    /** Categories get their id from the database, so tests set it by reflection. */
    private static Category category(long id, String name) {
        Category category = new Category(name, "Descripción de " + name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    void findAllReturnsCategoriesSortedByName() {
        when(categoryRepository.findAll(Sort.by("name")))
                .thenReturn(List.of(category(2, "Conectividad"), category(1, "Hardware")));

        List<CategoryResponse> result = service.findAll();

        assertThat(result).extracting(CategoryResponse::name).containsExactly("Conectividad", "Hardware");
    }

    @Test
    void findByIdThrowsNotFoundWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createTrimsNameAndStoresBlankDescriptionAsNull() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(new CategoryRequest("  Conectividad  ", "   "));

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Conectividad");
        assertThat(saved.getValue().getDescription()).isNull();
    }

    @Test
    void createRejectsDuplicateNameIgnoringCase() {
        when(categoryRepository.existsByNameIgnoreCase("conectividad")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CategoryRequest("conectividad", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(CategoryService.DUPLICATE_NAME);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateChangesNameAndDescription() {
        Category existing = category(1, "Redes");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        CategoryResponse result = service.update(1L, new CategoryRequest("Conectividad", "Internet y red local"));

        assertThat(result).isEqualTo(new CategoryResponse(1L, "Conectividad", "Internet y red local"));
    }

    @Test
    void updateRejectsNameUsedByAnotherCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1, "Redes")));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Hardware", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new CategoryRequest("Hardware", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(CategoryService.DUPLICATE_NAME);
    }

    @Test
    void deleteRejectsCategoryWithIncidents() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1, "Conectividad")));
        when(incidentRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessage(CategoryService.HAS_INCIDENTS);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesCategoryWithoutIncidents() {
        Category existing = category(1, "Conectividad");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(incidentRepository.existsByCategoryId(1L)).thenReturn(false);

        service.delete(1L);

        verify(categoryRepository).delete(existing);
    }
}
