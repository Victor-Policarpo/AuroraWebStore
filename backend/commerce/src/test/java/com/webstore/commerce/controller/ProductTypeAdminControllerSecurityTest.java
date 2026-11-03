package com.webstore.commerce.controller;

import com.webstore.commerce.config.SecurityConfig;
import com.webstore.commerce.dto.request.CreateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeRequest;
import com.webstore.commerce.dto.request.UpdateProductTypeStatusRequest;
import com.webstore.commerce.dto.response.ProductTypeResponse;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.service.ProductTypeService;
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

@WebMvcTest(controllers = ProductTypeAdminController.class, properties = "springdotenv.enabled=false")
@Import(SecurityConfig.class)
class ProductTypeAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductTypeService productTypeService;

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should list product types")
    void adminCanListProductTypes() throws Exception {
        when(productTypeService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/product-types"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should create product type")
    void adminCanCreateProductType() throws Exception {
        when(productTypeService.create(any(CreateProductTypeRequest.class))).thenReturn(productTypeResponse(true));

        mockMvc.perform(post("/api/v1/admin/product-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"GAME\",\"description\":\"Digital games\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should reject product type payload missing required fields")
    void adminCreateProductTypeWithInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/admin/product-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"description\":\"Digital games\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should get product type by ID")
    void adminCanGetProductTypeById() throws Exception {
        when(productTypeService.findById(any(UUID.class))).thenReturn(productTypeResponse(true));

        mockMvc.perform(get("/api/v1/admin/product-types/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should return 404 for a missing product type")
    void missingProductTypeReturnsNotFound() throws Exception {
        when(productTypeService.findById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Product type not found"));

        mockMvc.perform(get("/api/v1/admin/product-types/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should update product type")
    void adminCanUpdateProductType() throws Exception {
        when(productTypeService.update(any(UUID.class), any(UpdateProductTypeRequest.class)))
                .thenReturn(productTypeResponse(true));

        mockMvc.perform(patch("/api/v1/admin/product-types/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated description\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should update product type status")
    void adminCanUpdateProductTypeStatus() throws Exception {
        when(productTypeService.updateStatus(any(UUID.class), any(UpdateProductTypeStatusRequest.class)))
                .thenReturn(productTypeResponse(false));

        mockMvc.perform(patch("/api/v1/admin/product-types/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_CUSTOMER")
    @DisplayName("Customer should not access admin product type routes")
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/product-types"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_CUSTOMER")
    @DisplayName("Customer should not create product types")
    void customerCannotCreateProductType() throws Exception {
        mockMvc.perform(post("/api/v1/admin/product-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"GAME\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/product-types"))
                .andExpect(status().isUnauthorized());
    }

    private ProductTypeResponse productTypeResponse(boolean active) {
        return new ProductTypeResponse(
                UUID.randomUUID(), "GAME", "Digital games", active, Instant.now(), Instant.now());
    }
}
