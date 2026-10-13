package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateCategoryRequest;
import com.webstore.commerce.dto.request.UpdateCategoryRequest;
import com.webstore.commerce.dto.request.UpdateCategoryStatusRequest;
import com.webstore.commerce.dto.response.CategoryResponse;
import com.webstore.commerce.entity.Category;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.mapper.CategoryMapper;
import com.webstore.commerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
        categoryService = new CategoryService(categoryRepository, categoryMapper);
    }

    @Test
    @DisplayName("Should create category when name and slug are unique")
    void createCategory() {
        CreateCategoryRequest dto = new CreateCategoryRequest("Games", "Digital games", "games");
        when(categoryRepository.existsByName(dto.name())).thenReturn(false);
        when(categoryRepository.existsBySlug(dto.slug())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        CategoryResponse response = categoryService.create(dto);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        Category saved = captor.getValue();
        assertEquals("Games", saved.getName());
        assertEquals("Digital games", saved.getDescription());
        assertEquals("games", saved.getSlug());
        assertTrue(saved.isActive());
        assertNotNull(response.id());
        assertEquals("games", response.slug());
    }

    @Test
    @DisplayName("Should throw conflict when category name already exists")
    void createCategoryThrowsExceptionWhenNameExists() {
        CreateCategoryRequest dto = new CreateCategoryRequest("Games", "Digital games", "games");
        when(categoryRepository.existsByName(dto.name())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> categoryService.create(dto));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw conflict when category slug already exists")
    void createCategoryThrowsExceptionWhenSlugExists() {
        CreateCategoryRequest dto = new CreateCategoryRequest("Games", "Digital games", "games");
        when(categoryRepository.existsByName(dto.name())).thenReturn(false);
        when(categoryRepository.existsBySlug(dto.slug())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> categoryService.create(dto));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw not found when category does not exist")
    void findByIdThrowsExceptionWhenCategoryDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(id));
    }

    @Test
    @DisplayName("Should keep omitted fields unchanged on partial update")
    void updateCategoryOnlyChangesProvidedFields() {
        UUID id = UUID.randomUUID();
        Category category = category(id, "Games", "games", "Original description");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.update(
                id, new UpdateCategoryRequest(null, "Updated description", null));

        assertEquals("Games", response.name());
        assertEquals("games", response.slug());
        assertEquals("Updated description", response.description());
        verify(categoryRepository, never()).existsByNameAndIdNot(any(), any());
        verify(categoryRepository, never()).existsBySlugAndIdNot(any(), any());
    }

    @Test
    @DisplayName("Should throw conflict when updated slug already belongs to another category")
    void updateCategoryThrowsExceptionWhenSlugConflicts() {
        UUID id = UUID.randomUUID();
        Category category = category(id, "Games", "games", "Original description");
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlugAndIdNot("new-slug", id)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> categoryService.update(id, new UpdateCategoryRequest(null, null, "new-slug")));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Should deactivate category when status update is requested")
    void updateStatusDeactivatesCategory() {
        UUID id = UUID.randomUUID();
        Category category = category(id, "Games", "games", "Original description");
        category.setActive(true);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.updateStatus(id, new UpdateCategoryStatusRequest(false));

        assertFalse(response.active());
        assertFalse(category.isActive());
        verify(categoryRepository).save(category);
    }

    private Category category(UUID id, String name, String slug, String description) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        return category;
    }
}
