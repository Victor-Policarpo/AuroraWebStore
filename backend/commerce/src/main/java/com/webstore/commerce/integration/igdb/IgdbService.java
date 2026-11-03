package com.webstore.commerce.integration.igdb;

import com.webstore.commerce.dto.response.IgdbGameSearchResponse;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.integration.igdb.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IgdbService {

    private final IgdbClient igdbClient;

    public List<IgdbGameSearchResponse> searchGames(String searchTerm) {
        return igdbClient.searchGames(searchTerm)
                .stream()
                .map(this::toSearchResponse)
                .toList();
    }

    private IgdbGameSearchResponse toSearchResponse(IgdbGameResponse game) {
        String coverUrl = null;

        if (game.cover() != null && game.cover().url() != null) {
            coverUrl = normalizeImageUrl(game.cover().url());
        }

        List<String> genres = extractNames(game.genres());
        List<String> platforms = extractNames(game.platforms());

        return new IgdbGameSearchResponse(
                game.id(),
                game.name(),
                game.summary(),
                coverUrl,
                genres,
                platforms
        );
    }

    public IgdbGetGameResponse getGame(Long igdbId) {
        return mapToResponse(igdbClient.getGame(igdbId));
    }

    public IgdbGetGameResponse mapToResponse(IgdbGame dto) {
        String coverImageId = dto.cover() != null
                ? dto.cover().image_id()
                : null;

        List<String> artworksImageIds = extractImageIds(dto.artworks());
        List<String> screenshotsImageIds = extractImageIds(dto.screenshots());

        String trailerUrl = null;

        if (dto.videos() != null && !dto.videos().isEmpty()) {
            String videoId = dto.videos().getFirst().video_id();

            if (videoId != null && !videoId.isBlank()) {
                trailerUrl = "https://www.youtube.com/watch?v=" + videoId;
            }
        }

        List<String> genres = extractNames(dto.genres());
        List<String> platforms = extractNames(dto.platforms());

        List<IgdbCompanyResponse> companies =
                dto.involved_companies() != null
                        ? dto.involved_companies().stream()
                        .filter(ic -> ic != null
                                && ic.company() != null
                                && ic.company().name() != null)
                        .map(ic -> new IgdbCompanyResponse(
                                ic.company().name(),
                                ic.developer(),
                                ic.publisher()
                        ))
                        .toList()
                        : List.of();

        return new IgdbGetGameResponse(
                dto.id(),
                dto.name(),
                dto.summary(),
                trailerUrl,
                coverImageId,
                artworksImageIds,
                screenshotsImageIds,
                genres,
                platforms,
                companies
        );
    }

    private List<String> extractImageIds(List<IgdbImage> images) {
        if (images == null) {
            return List.of();
        }

        return images.stream()
                .filter(image -> image != null && image.image_id() != null)
                .map(IgdbImage::image_id)
                .toList();
    }

    private List<String> extractNames(List<IgdbNamedResponse> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .filter(item -> item != null && item.name() != null)
                .map(IgdbNamedResponse::name)
                .toList();
    }

    private String normalizeImageUrl(String url) {
       if (url == null || url.isBlank()) {
           throw new ExternalApiException("IGDB image URL is null or blank");
       }
        return url.startsWith("//") ? "https:" + url : url;
    }
}