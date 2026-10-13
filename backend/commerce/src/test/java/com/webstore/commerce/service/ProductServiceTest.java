package com.webstore.commerce.service;

import com.webstore.commerce.dto.request.CreateProductRequest;
import com.webstore.commerce.dto.request.UpdateProductRequest;
import com.webstore.commerce.dto.request.UpdateProductStatusRequest;
import com.webstore.commerce.dto.response.ProductResponse;
import com.webstore.commerce.entity.Category;
import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.enums.ProductStatus;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.mapper.ProductMapper;
import com.webstore.commerce.repository.CategoryRepository;
import com.webstore.commerce.repository.ProductRepository;
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
class ProductServiceTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID OTHER_CATEGORY_ID = UUID.randomUUID();

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
        productService = new ProductService(productRepository, categoryRepository, productMapper);
    }

    @Test
    @DisplayName("Should create a product with DRAFT as initial status when slug is unique")
    void createProduct() {
        CreateProductRequest dto = createRequest(null);
        when(productRepository.existsBySlug(dto.slug())).thenReturn(false);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        ProductResponse response = productService.create(dto);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertEquals("Cyberpunk 2077", saved.getName());
        assertEquals("cyberpunk-2077", saved.getSlug());
        assertEquals(CATEGORY_ID, saved.getCategory().getId());
        assertEquals(ProductStatus.DRAFT, saved.getStatus());
        assertNotNull(response.id());
        assertEquals(CATEGORY_ID, response.categoryId());
        assertEquals(ProductStatus.DRAFT, response.status());
        verify(productRepository, never()).existsByIgdbId(any());
    }

    @Test
    @DisplayName("Should reject duplicated IGDB ID on creation")
    void createProductThrowsExceptionWhenIgdbIdExists() {
        CreateProductRequest dto = createRequest(123L);
        when(productRepository.existsBySlug(dto.slug())).thenReturn(false);
        when(productRepository.existsByIgdbId(123L)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> productService.create(dto));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw conflict when product slug already exists")
    void createProductThrowsExceptionWhenSlugExists() {
        CreateProductRequest dto = createRequest(null);
        when(productRepository.existsBySlug(dto.slug())).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> productService.create(dto));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw not found when referenced category does not exist")
    void createProductThrowsExceptionWhenCategoryDoesNotExist() {
        CreateProductRequest dto = createRequest(null);
        when(productRepository.existsBySlug(dto.slug())).thenReturn(false);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.create(dto));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw not found when product does not exist")
    void findByIdThrowsExceptionWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(id));
    }

    @Test
    @DisplayName("Should keep omitted fields and status unchanged on partial update")
    void updateProductOnlyChangesProvidedFields() {
        UUID id = UUID.randomUUID();
        Product product = product(id, ProductStatus.ACTIVE);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.update(
                id, new UpdateProductRequest("New Name", "New description", null, null, null, null,
                        null, null, null, null, null));

        assertEquals("New Name", response.name());
        assertEquals("New description", response.description());
        assertEquals("old-slug", response.slug());
        assertEquals(100L, response.basePrice());
        assertEquals(50L, response.igdbId());
        assertEquals(CATEGORY_ID, response.categoryId());
        assertEquals(ProductStatus.ACTIVE, response.status());
        verify(productRepository, never()).existsBySlugAndIdNot(any(), any());
        verify(productRepository, never()).existsByIgdbIdAndIdNot(any(), any());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should throw conflict when updated slug belongs to another product")
    void updateProductThrowsExceptionWhenSlugConflicts() {
        UUID id = UUID.randomUUID();
        Product product = product(id, ProductStatus.DRAFT);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugAndIdNot("new-slug", id)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> productService.update(
                id, new UpdateProductRequest(null, null, "new-slug", null, null, null,
                        null, null, null, null, null)));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw conflict when updated IGDB ID belongs to another product")
    void updateProductThrowsExceptionWhenIgdbIdConflicts() {
        UUID id = UUID.randomUUID();
        Product product = product(id, ProductStatus.DRAFT);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.existsByIgdbIdAndIdNot(999L, id)).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> productService.update(
                id, new UpdateProductRequest(null, null, null, null, null, null,
                        null, null, 999L, null, null)));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw not found when changing to a nonexistent category")
    void updateProductThrowsExceptionWhenCategoryDoesNotExist() {
        UUID id = UUID.randomUUID();
        Product product = product(id, ProductStatus.DRAFT);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(
                id, new UpdateProductRequest(null, null, null, null, null, null,
                        null, null, null, null, OTHER_CATEGORY_ID)));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should assign the new category when categoryId is provided on update")
    void updateProductAssignsNewCategory() {
        UUID id = UUID.randomUUID();
        Product product = product(id, ProductStatus.DRAFT);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(category(OTHER_CATEGORY_ID)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.update(
                id, new UpdateProductRequest(null, null, null, null, null, null,
                        null, null, null, null, OTHER_CATEGORY_ID));

        assertEquals(OTHER_CATEGORY_ID, response.categoryId());
        assertEquals(OTHER_CATEGORY_ID, product.getCategory().getId());
    }

    @Test
    @DisplayName("Should update product status through the dedicated operation")
    void updateStatusSetsRequestedStatus() {
        UUID id = UUID.randomUUID();
        Product product = product(id, ProductStatus.DRAFT);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateStatus(
                id, new UpdateProductStatusRequest(ProductStatus.ACTIVE));

        assertEquals(ProductStatus.ACTIVE, product.getStatus());
        assertEquals(ProductStatus.ACTIVE, response.status());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should throw not found when updating status of a missing product")
    void updateStatusThrowsExceptionWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.updateStatus(
                id, new UpdateProductStatusRequest(ProductStatus.ACTIVE)));
    }

    private CreateProductRequest createRequest(Long igdbId) {
        return new CreateProductRequest(
                CATEGORY_ID, "Cyberpunk 2077", "A role-playing game", "cyberpunk-2077",
                "CD Projekt", "PC", "GLOBAL", 24999L, "BRL", igdbId, null);
    }

    private Category category(UUID id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private Product product(UUID id, ProductStatus status) {
        Product product = new Product();
        product.setId(id);
        product.setCategory(category(CATEGORY_ID));
        product.setName("Old Name");
        product.setDescription("Old description");
        product.setSlug("old-slug");
        product.setBrand("Old brand");
        product.setPlatform("PC");
        product.setRegion("GLOBAL");
        product.setBasePrice(100L);
        product.setBaseCurrency("BRL");
        product.setStatus(status);
        product.setIgdbId(50L);
        product.setSteamAppId(40L);
        return product;
    }
}