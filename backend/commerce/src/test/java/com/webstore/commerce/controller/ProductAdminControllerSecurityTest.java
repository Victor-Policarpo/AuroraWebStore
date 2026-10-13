package com.webstore.commerce.controller;

import com.webstore.commerce.config.SecurityConfig;
import com.webstore.commerce.dto.request.CreateProductRequest;
import com.webstore.commerce.dto.request.UpdateProductStatusRequest;
import com.webstore.commerce.dto.response.ProductResponse;
import com.webstore.commerce.entity.enums.ProductStatus;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductAdminController.class, properties = "springdotenv.enabled=false")
@Import(SecurityConfig.class)
class ProductAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should list products")
    void adminCanListProducts() throws Exception {
        when(productService.findAll(any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should create product")
    void adminCanCreateProduct() throws Exception {
        when(productService.create(any(CreateProductRequest.class))).thenReturn(productResponse(ProductStatus.DRAFT));

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","name":"Cyberpunk 2077","description":"A game","slug":"cyberpunk-2077","brand":"CD Projekt","platform":"PC","region":"GLOBAL","basePrice":24999,"baseCurrency":"BRL"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should reject product payload with missing or invalid fields")
    void adminCreateProductWithInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","name":"","slug":"cyberpunk-2077","platform":"PC","region":"GLOBAL","basePrice":-5,"baseCurrency":"JPY"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should update product status")
    void adminCanUpdateProductStatus() throws Exception {
        when(productService.updateStatus(any(UUID.class), any(UpdateProductStatusRequest.class)))
                .thenReturn(productResponse(ProductStatus.ACTIVE));

        mockMvc.perform(patch("/api/v1/admin/products/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should reject an invalid product status value")
    void adminUpdateProductStatusWithInvalidValue() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/products/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should return 404 for a missing product")
    void missingProductReturnsNotFound() throws Exception {
        when(productService.findById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/v1/admin/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_CUSTOMER")
    @DisplayName("Customer should not access admin product routes")
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_CUSTOMER")
    @DisplayName("Customer should not create products")
    void customerCannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":"%s","name":"Cyberpunk 2077","slug":"cyberpunk-2077","platform":"PC","region":"GLOBAL","basePrice":24999,"baseCurrency":"BRL"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    private ProductResponse productResponse(ProductStatus status) {
        return new ProductResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Cyberpunk 2077", "A game", "cyberpunk-2077",
                "CD Projekt", "PC", "GLOBAL", 24999L, "BRL", status,
                null, null, Instant.now(), Instant.now());
    }
}