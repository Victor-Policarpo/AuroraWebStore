package com.webstore.commerce.controller;

import com.webstore.commerce.dto.response.IgdbGameSearchResponse;
import com.webstore.commerce.integration.igdb.IgdbClient;
import com.webstore.commerce.integration.igdb.IgdbService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/igdb")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
public class IgdbController {

    private final IgdbService igdbService;

    public IgdbController(IgdbService igdbService) {
        this.igdbService = igdbService;
    }

    @GetMapping("/games")
    public List<IgdbGameSearchResponse> searchGames(
            @RequestParam
            @NotBlank(message = "must not be blank")
            @Size(min = IgdbClient.MIN_SEARCH_LENGTH, max = IgdbClient.MAX_SEARCH_LENGTH,
                    message = "must contain between " + IgdbClient.MIN_SEARCH_LENGTH
                            + " and " + IgdbClient.MAX_SEARCH_LENGTH + " characters")
            String search
    ) {
        return igdbService.searchGames(search);
    }
}
