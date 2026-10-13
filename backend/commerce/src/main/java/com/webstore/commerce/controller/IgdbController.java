package com.webstore.commerce.controller;

import com.webstore.commerce.dto.response.IgdbGameSearchResponse;
import com.webstore.commerce.integration.igdb.IgdbService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/igdb")
public class IgdbController {

    private final IgdbService igdbService;

    public IgdbController(IgdbService igdbService) {
        this.igdbService = igdbService;
    }

    @GetMapping("/games")
    public List<IgdbGameSearchResponse> searchGames(
            @RequestParam String search
    ) {
        return igdbService.searchGames(search);
    }
}