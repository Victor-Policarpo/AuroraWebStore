package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeStatusRequest;
import com.webstore.commerce.dto.response.ProductTypeResponse;
import com.webstore.commerce.entity.ProductType;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.mapper.ProductTypeMapper;
import com.webstore.commerce.repository.ProductTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductTypeServiceTest {

    @Mock
    private ProductTypeRepository productTypeRepository;

    private ProductTypeService productTypeService;

    @BeforeEach
    void setUp() {
        ProductTypeMapper productTypeMapper = Mappers.getMapper(ProductTypeMapper.class);
        productTypeService = new ProductTypeService(productTypeRepository, productTypeMapper);
    }

    @Test
    @DisplayName("Should create product type when name is unique")
    void createProductType() {
        CreateProductTypeRequest dto = new CreateProductTypeRequest("GAME", "Digital games");
        when(productTypeRepository.existsByName(dto.name())).thenReturn(false);
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> {
            ProductType toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        ProductTypeResponse response = productTypeService.create(dto);

        ArgumentCaptor<ProductType> captor = ArgumentCaptor.forClass(ProductType.class);
        verify(productTypeRepository).save(captor.capture());
        ProductType saved = captor.getValue();
        assertEquals("GAME", saved.getName());
        assertEquals("Digital games", saved.getDescription());
        assertTrue(saved.isActive());
        assertNotNull(response.id());
        assertEquals("GAME", response.name());
    }

    @Test
    @DisplayName("Should throw conflict when product type name already exists")
    void createProductTypeThrowsExceptionWhenNameExists() {
        CreateProductTypeRequest dto = new CreateProductTypeRequest("GAME", "Digital games");
        when(productTypeRepository.existsByName(dto.name())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> productTypeService.create(dto));
        verify(productTypeRepository, never()).save(any(ProductType.class));
    }

    @Test
    @DisplayName("Should list product types")
    void findAllProductTypes() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productTypeRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<ProductTypeResponse> result = productTypeService.findAll(pageable);

        assertTrue(result.isEmpty());
        verify(productTypeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should throw not found when product type does not exist")
    void findByIdThrowsExceptionWhenProductTypeDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productTypeService.findById(id));
    }

    @Test
    @DisplayName("Should return product type by ID")
    void findByIdReturnsProductType() {
        UUID id = UUID.randomUUID();
        when(productTypeRepository.findById(id)).thenReturn(Optional.of(productType(id, "GAME", true)));

        ProductTypeResponse response = productTypeService.findById(id);

        assertEquals(id, response.id());
        assertEquals("GAME", response.name());
    }

    @Test
    @DisplayName("Should keep omitted fields unchanged on partial update")
    void updateProductTypeOnlyChangesProvidedFields() {
        UUID id = UUID.randomUUID();
        ProductType productType = productType(id, "GAME", true);
        productType.setDescription("Original description");
        when(productTypeRepository.findById(id)).thenReturn(Optional.of(productType));
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductTypeResponse response = productTypeService.update(
                id, new UpdateProductTypeRequest(null, "Updated description"));

        assertEquals("GAME", response.name());
        assertEquals("Updated description", response.description());
        verify(productTypeRepository, never()).existsByNameAndIdNot(any(), any());
    }

    @Test
    @DisplayName("Should throw conflict when updated name belongs to another product type")
    void updateProductTypeThrowsExceptionWhenNameConflicts() {
        UUID id = UUID.randomUUID();
        ProductType productType = productType(id, "GAME", true);
        when(productTypeRepository.findById(id)).thenReturn(Optional.of(productType));
        when(productTypeRepository.existsByNameAndIdNot("GIFT_CARD", id)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class,
                () -> productTypeService.update(id, new UpdateProductTypeRequest("GIFT_CARD", null)));
        verify(productTypeRepository, never()).save(any(ProductType.class));
    }

    @Test
    @DisplayName("Should deactivate product type when status update is requested")
    void updateStatusDeactivatesProductType() {
        UUID id = UUID.randomUUID();
        ProductType productType = productType(id, "GAME", true);
        when(productTypeRepository.findById(id)).thenReturn(Optional.of(productType));
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductTypeResponse response = productTypeService.updateStatus(
                id, new UpdateProductTypeStatusRequest(false));

        assertFalse(response.active());
        assertFalse(productType.isActive());
        verify(productTypeRepository).save(productType);
    }

    @Test
    @DisplayName("Should activate product type when status update is requested")
    void updateStatusActivatesProductType() {
        UUID id = UUID.randomUUID();
        ProductType productType = productType(id, "GAME", false);
        when(productTypeRepository.findById(id)).thenReturn(Optional.of(productType));
        when(productTypeRepository.save(any(ProductType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductTypeResponse response = productTypeService.updateStatus(
                id, new UpdateProductTypeStatusRequest(true));

        assertTrue(response.active());
        assertTrue(productType.isActive());
    }

    @Test
    @DisplayName("Product type must not expose a slug")
    void productTypeDoesNotHaveSlug() {
        assertThrows(NoSuchFieldException.class, () -> ProductType.class.getDeclaredField("slug"));
        assertThrows(NoSuchFieldException.class, () -> ProductTypeResponse.class.getDeclaredField("slug"));
    }

    private ProductType productType(UUID id, String name, boolean active) {
        ProductType productType = new ProductType();
        productType.setId(id);
        productType.setName(name);
        productType.setActive(active);
        return productType;
    }
}
