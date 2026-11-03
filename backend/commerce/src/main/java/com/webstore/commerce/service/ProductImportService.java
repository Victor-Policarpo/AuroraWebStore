package com.webstore.commerce.service;

import com.webstore.commerce.dto.response.ProductImportResponse;
import com.webstore.commerce.entity.Category;
import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.ProductType;
import com.webstore.commerce.entity.enums.ProductStatus;
import com.webstore.commerce.exception.errors.BadRequestException;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.integration.igdb.IgdbService;
import com.webstore.commerce.integration.igdb.dto.IgdbCompanyResponse;
import com.webstore.commerce.integration.igdb.dto.IgdbGetGameResponse;
import com.webstore.commerce.repository.CategoryRepository;
import com.webstore.commerce.repository.ProductRepository;
import com.webstore.commerce.repository.ProductTypeRepository;
import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.Normalizer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class ProductImportService {
    private static final Logger LOG = Logger.getLogger(ProductImportService.class.getName());
    private static final int MAX_IMAGES = ProductImageService.MAX_IMPORTED_IMAGES;
    private static final Map<String, String> GENRE_MAP = Map.ofEntries(
            Map.entry("role-playing (rpg)", "RPG"), Map.entry("rpg", "RPG"),
            Map.entry("hack and slash/beat 'em up", "Ação"), Map.entry("fighting", "Luta"),
            Map.entry("shooter", "Tiro"), Map.entry("racing", "Corrida"),
            Map.entry("strategy", "Estratégia"), Map.entry("adventure", "Aventura"),
            Map.entry("simulator", "Simulação"), Map.entry("sport", "Esporte"),
            Map.entry("puzzle", "Quebra-cabeça"), Map.entry("platform", "Plataforma"),
            Map.entry("arcade", "Arcade"), Map.entry("indie", "Indie"), Map.entry("music", "Música")
    );
    private final IgdbService igdbService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductTypeRepository productTypeRepository;
    private final ProductImageService productImageService;
    private final IgdbImageDownloader imageDownloader;
    private final PlatformTransactionManager transactionManager;

    public ProductImportResponse importGame(Long igdbId) {
        if (igdbId == null || igdbId <= 0) throw new BadRequestException("IGDB ID must be positive");
        if (productRepository.existsByIgdbId(igdbId))
            throw new ResourceAlreadyExistsException("Product with this IGDB ID already exists");

        IgdbGetGameResponse game = igdbService.getGame(igdbId);
        if (game == null || game.name() == null || game.name().isBlank()
                || game.summary() == null || game.summary().isBlank())
            throw new BadRequestException("IGDB game must include a name and summary");
        ProductType type = productTypeRepository.findByName("GAME")
                .orElseThrow(() -> new ResourceNotFoundException("GAME product type is not configured"));
        if (!type.isActive()) throw new BadRequestException("GAME product type is inactive");

        List<String> unmatched = new ArrayList<>();
        Map<String, String> mappedGenres = new LinkedHashMap<>();
        for (String genre : Optional.ofNullable(game.genres()).orElse(List.of())) {
            String mapped = genre == null ? null : GENRE_MAP.get(genre.trim().toLowerCase(Locale.ROOT));
            if (mapped == null) unmatched.add(genre);
            else mappedGenres.putIfAbsent(genre, mapped);
        }
        List<Category> found = mappedGenres.isEmpty() ? List.of()
                : categoryRepository.findByNameInAndActiveTrue(new HashSet<>(mappedGenres.values()));
        Set<String> foundNames = new HashSet<>();
        found.forEach(c -> foundNames.add(c.getName().toLowerCase(Locale.ROOT)));
        mappedGenres.forEach((genre, category) -> {
            if (!foundNames.contains(category.toLowerCase(Locale.ROOT))) unmatched.add(genre);
        });

        Product product = new Product();
        product.setName(game.name());
        product.setDescription(game.summary());
        product.setSlug(uniqueSlug(game.name(), igdbId));
        product.setBrand(resolveBrand(game.companies()));
        product.setStatus(ProductStatus.DRAFT);
        product.setIgdbId(igdbId);
        product.setTrailerUrl(game.trailerUrl());
        product.setProductType(type);
        product.setCategories(new LinkedHashSet<>(found));

        List<String> imageIds = selectImageIds(game.coverImageId(), game.artworksImageIds(), game.screenshotsImageIds());
        List<IgdbImageDownloader.DownloadedImage> images = new ArrayList<>();
        for (String imageId : imageIds.stream().limit(MAX_IMAGES).toList())
            images.add(imageDownloader.download(imageId, "t_1080p"));

        List<String> uploaded = new ArrayList<>();
        try {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.executeWithoutResult(status -> {
                productRepository.saveAndFlush(product);
                for (int i = 0; i < images.size(); i++) {
                    var image = images.get(i);
                    uploaded.add(productImageService.uploadImportedImage(
                            image.bytes(), image.contentType(), image.filename(), product, i));
                }
            });
        } catch (RuntimeException e) {
            try { productImageService.deleteStoredObjects(uploaded); }
            catch (RuntimeException cleanup) {
                e.addSuppressed(cleanup);
                LOG.log(Level.SEVERE, "Failed to clean up S3 objects after IGDB import failure: " + uploaded, cleanup);
            }
            if (e instanceof DataIntegrityViolationException || e instanceof PersistenceException)
                throw new ResourceAlreadyExistsException("The product conflicts with an existing catalog record");
            throw e;
        }
        return new ProductImportResponse(product.getId(), igdbId, product.getName(), product.getStatus(),
                found.stream().map(Category::getName).toList(), List.copyOf(unmatched));
    }

    static List<String> selectImageIds(String coverId, List<String> artworks, List<String> screenshots) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        if (coverId != null && !coverId.isBlank()) selected.add(coverId);

        List<String> uniqueArtworks = distinctIds(artworks);
        List<String> uniqueScreenshots = distinctIds(screenshots);
        addCandidates(selected, uniqueArtworks, 4, MAX_IMAGES);
        addCandidates(selected, uniqueScreenshots, 4, MAX_IMAGES);
        addCandidates(selected, uniqueScreenshots, MAX_IMAGES, MAX_IMAGES);
        addCandidates(selected, uniqueArtworks, MAX_IMAGES, MAX_IMAGES);
        return List.copyOf(selected);
    }

    private static List<String> distinctIds(List<String> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
    }

    private static void addCandidates(LinkedHashSet<String> selected, List<String> candidates,
                                     int candidateLimit, int imageLimit) {
        int added = 0;
        for (String id : candidates) {
            if (selected.size() >= imageLimit || added >= candidateLimit) break;
            if (selected.add(id)) added++;
        }
    }

    private String resolveBrand(List<IgdbCompanyResponse> companies) {
        if (companies == null) return "Unknown";
        return companies.stream().filter(c -> c != null && Boolean.TRUE.equals(c.developer()))
                .map(IgdbCompanyResponse::name).filter(n -> n != null && !n.isBlank()).findFirst()
                .orElseGet(() -> companies.stream().filter(c -> c != null && Boolean.TRUE.equals(c.publisher()))
                        .map(IgdbCompanyResponse::name).filter(n -> n != null && !n.isBlank()).findFirst().orElse(null));
    }

    private String uniqueSlug(String name, Long id) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (base.isBlank()) base = "game-" + id;
        return productRepository.existsBySlug(base) ? base + "-" + id : base;
    }
}
