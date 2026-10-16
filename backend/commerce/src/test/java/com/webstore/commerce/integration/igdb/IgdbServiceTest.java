package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.dto.response.IgdbGameSearchResponse;
import com.webstore.commerce.integration.igdb.dto.IgdbCoverResponse;
import com.webstore.commerce.integration.igdb.dto.IgdbGameResponse;
import com.webstore.commerce.integration.igdb.dto.IgdbNamedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IgdbServiceTest {

    private final IgdbClient igdbClient = mock(IgdbClient.class);
    private final IgdbService igdbService = new IgdbService(igdbClient);

    @Test
    @DisplayName("Should map IGDB results to the search response")
    void mapsSearchResults() {
        IgdbGameResponse game = new IgdbGameResponse(
                42L,
                "Zelda",
                "An adventure",
                new IgdbCoverResponse("https://images.igdb.com/cover.jpg"),
                List.of(new IgdbNamedResponse("Adventure"), new IgdbNamedResponse("RPG")),
                List.of(new IgdbNamedResponse("Switch")),
                95.5,
                123456L);

        when(igdbClient.searchGames("zelda")).thenReturn(List.of(game));

        List<IgdbGameSearchResponse> response = igdbService.searchGames("zelda");

        assertEquals(1, response.size());
        assertEquals(42L, response.get(0).igdbId());
        assertEquals("Zelda", response.get(0).name());
        assertEquals("An adventure", response.get(0).summary());
        assertEquals("https://images.igdb.com/cover.jpg", response.get(0).coverUrl());
        assertEquals(List.of("Adventure", "RPG"), response.get(0).genres());
        assertEquals(List.of("Switch"), response.get(0).platforms());
    }

    @Test
    @DisplayName("Should normalize protocol relative cover URLs to https")
    void normalizesProtocolRelativeCoverUrl() {
        IgdbGameResponse game = new IgdbGameResponse(
                1L, "Zelda", null,
                new IgdbCoverResponse("//images.igdb.com/cover.jpg"),
                null, null, null, null);

        when(igdbClient.searchGames("zelda")).thenReturn(List.of(game));

        assertEquals("https://images.igdb.com/cover.jpg",
                igdbService.searchGames("zelda").get(0).coverUrl());
    }

    @Test
    @DisplayName("Should tolerate missing cover and null collections")
    void toleratesMissingOptionalFields() {
        IgdbGameResponse game = new IgdbGameResponse(
                1L, "Zelda", null, null, null, null, null, null);

        when(igdbClient.searchGames("zelda")).thenReturn(List.of(game));

        IgdbGameSearchResponse response = igdbService.searchGames("zelda").get(0);

        assertNull(response.coverUrl());
        assertTrue(response.genres().isEmpty());
        assertTrue(response.platforms().isEmpty());
    }

    @Test
    @DisplayName("Should ignore null named entries and blank names")
    void ignoresInvalidNamedEntries() {
        IgdbGameResponse game = new IgdbGameResponse(
                1L, "Zelda", null, null,
                Arrays.asList(new IgdbNamedResponse("Adventure"), null),
                List.of(),
                null, null);

        when(igdbClient.searchGames("zelda")).thenReturn(List.of(game));

        assertEquals(List.of("Adventure"), igdbService.searchGames("zelda").get(0).genres());
    }
}
