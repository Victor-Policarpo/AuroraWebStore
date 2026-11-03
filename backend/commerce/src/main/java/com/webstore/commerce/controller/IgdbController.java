package com.webstore.commerce.controller;

import com.webstore.commerce.dto.response.IgdbGameSearchResponse;
import com.webstore.commerce.integration.igdb.IgdbClient;
import com.webstore.commerce.integration.igdb.IgdbService;
import com.webstore.commerce.dto.request.ImportIgdbGameRequest;
import com.webstore.commerce.dto.response.ProductImportResponse;
import com.webstore.commerce.service.ProductImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/igdb")
@PreAuthorize("hasAuthority('SCOPE_ADMIN')")
@RequiredArgsConstructor
public class IgdbController {

    private final IgdbService igdbService;
    private final ProductImportService productImportService;

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

    @PostMapping("/import")
    public ResponseEntity<ProductImportResponse> importGame(@Valid @RequestBody ImportIgdbGameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productImportService.importGame(request.igdbId()));
    }
}
