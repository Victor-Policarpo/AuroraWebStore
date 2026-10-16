package com.webstore.commerce.controller;

import com.webstore.commerce.config.SecurityConfig;
import com.webstore.commerce.dto.response.IgdbGameSearchResponse;
import com.webstore.commerce.integration.igdb.IgdbService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IgdbController.class, properties = "springdotenv.enabled=false")
@Import(SecurityConfig.class)
class IgdbControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IgdbService igdbService;

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Admin should search games")
    void adminCanSearchGames() throws Exception {
        when(igdbService.searchGames("zelda")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/igdb/games").param("search", "zelda"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should reject a too short search term")
    void adminSearchWithInvalidTerm() throws Exception {
        mockMvc.perform(get("/api/v1/admin/igdb/games").param("search", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"));

        verifyNoInteractions(igdbService);
    }

    @Test
    @WithMockUser(authorities = "SCOPE_CUSTOMER")
    @DisplayName("Customer should not access the IGDB admin search")
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/igdb/games").param("search", "zelda"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous user should be unauthorized")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/igdb/games").param("search", "zelda"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_ADMIN")
    @DisplayName("Should return results through the controller")
    void adminReceivesResults() throws Exception {
        when(igdbService.searchGames(anyString())).thenReturn(List.of(
                new IgdbGameSearchResponse(1L, "Zelda", "Adventure", null, List.of(), List.of())));

        mockMvc.perform(get("/api/v1/admin/igdb/games").param("search", "zelda"))
                .andExpect(status().isOk());
    }
}
