package com.webstore.commerce.controller;

import com.webstore.commerce.config.SecurityConfig;
import com.webstore.commerce.dto.request.CreateCategoryRequest;
import com.webstore.commerce.dto.response.CategoryResponse;
import com.webstore.commerce.service.CategoryService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryAdminController.class, properties = "springdotenv.enabled=false")
@Import(SecurityConfig.class)
class CategoryAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should list categories")
    void adminCanListCategories() throws Exception {
        when(categoryService.findAll(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should create category")
    void adminCanCreateCategory() throws Exception {
        CategoryResponse response = new CategoryResponse(
                UUID.randomUUID(), "Games", "desc", "games", true, Instant.now(), Instant.now());
        when(categoryService.create(any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Games\",\"description\":\"desc\",\"slug\":\"games\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should reject payload missing required fields")
    void adminCreateCategoryWithInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"slug\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_CUSTOMER")
    @DisplayName("Customer should not access admin category routes")
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_CUSTOMER")
    @DisplayName("Customer should not create categories")
    void customerCannotCreateCategory() throws Exception {
        mockMvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Games\",\"slug\":\"games\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isUnauthorized());
    }
}
