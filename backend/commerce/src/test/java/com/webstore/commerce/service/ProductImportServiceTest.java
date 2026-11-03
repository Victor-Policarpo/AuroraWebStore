package com.webstore.commerce.service;

import com.webstore.commerce.entity.Category;
import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.ProductType;
import com.webstore.commerce.entity.enums.ProductStatus;
import com.webstore.commerce.exception.errors.BadRequestException;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import com.webstore.commerce.integration.igdb.IgdbService;
import com.webstore.commerce.integration.igdb.dto.IgdbGetGameResponse;
import com.webstore.commerce.repository.CategoryRepository;
import com.webstore.commerce.repository.ProductRepository;
import com.webstore.commerce.repository.ProductTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceTest {
    @Mock IgdbService igdbService;
    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ProductTypeRepository productTypeRepository;
    @Mock ProductImageService productImageService;
    @Mock IgdbImageDownloader downloader;
    @Mock PlatformTransactionManager transactionManager;
    ProductImportService service;

    @BeforeEach void setup() {
        service = new ProductImportService(igdbService, productRepository, categoryRepository,
                productTypeRepository, productImageService, downloader, transactionManager);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(i -> i.getArgument(0));
    }
    private IgdbGetGameResponse game() {
        return new IgdbGetGameResponse(44L, "Sample Game", "A valid summary", null,
                null, List.of(), List.of(), List.of("RPG", "Mystery"), List.of(), List.of());
    }
    private ProductType type(boolean active) {
        ProductType p = new ProductType(); p.setName("GAME"); p.setActive(active); return p;
    }
    @Test void importsDraftWithNullCommercialFieldsAndUnmatchedGenre() {
        when(productRepository.existsByIgdbId(44L)).thenReturn(false);
        when(productRepository.existsBySlug("sample-game")).thenReturn(false);
        when(igdbService.getGame(44L)).thenReturn(game());
        when(productTypeRepository.findByName("GAME")).thenReturn(Optional.of(type(true)));
        Category rpg = new Category(); rpg.setName("RPG"); rpg.setActive(true);
        when(categoryRepository.findByNameInAndActiveTrue(anyCollection())).thenReturn(List.of(rpg));
        var result = service.importGame(44L);
        assertEquals(ProductStatus.DRAFT, result.status());
        assertEquals(List.of("RPG"), result.categoriesAssociated());
        assertEquals(List.of("Mystery"), result.unmatchedGenres());
        verify(productRepository).saveAndFlush(argThat(p -> p.getBasePrice() == null && p.getBaseCurrency() == null
                && p.getPlatform() == null && p.getRegion() == null && p.getSteamAppId() == null));
    }
    @Test void rejectsDuplicateBeforeIgdbCall() {
        when(productRepository.existsByIgdbId(44L)).thenReturn(true);
        assertThrows(ResourceAlreadyExistsException.class, () -> service.importGame(44L));
        verifyNoInteractions(igdbService);
    }
    @Test void rejectsMissingAndInactiveGameType() {
        when(productRepository.existsByIgdbId(44L)).thenReturn(false);
        when(igdbService.getGame(44L)).thenReturn(game());
        when(productTypeRepository.findByName("GAME")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.importGame(44L));
        when(productTypeRepository.findByName("GAME")).thenReturn(Optional.of(type(false)));
        assertThrows(BadRequestException.class, () -> service.importGame(44L));
    }
    @Test void rejectsMetadataWithoutNameOrSummary() {
        when(productRepository.existsByIgdbId(44L)).thenReturn(false);
        when(igdbService.getGame(44L)).thenReturn(new IgdbGetGameResponse(44L, " ", null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThrows(BadRequestException.class, () -> service.importGame(44L));
    }

    @Test void capsImagesAtTwelveAndPreservesCoverArtworkAndScreenshotPriority() {
        when(productRepository.existsByIgdbId(44L)).thenReturn(false);
        when(productRepository.existsBySlug("sample-game")).thenReturn(false);
        var metadata = new IgdbGetGameResponse(44L, "Sample Game", "A valid summary", "https://youtube.test/v",
                "cover", List.of("art1", "art2", "art3", "art4", "art5", "art6", "art7"),
                List.of("screen1", "screen2", "screen3", "screen4", "screen5", "screen6", "screen7", "screen8"),
                List.of(), List.of(), List.of());
        when(igdbService.getGame(44L)).thenReturn(metadata);
        when(productTypeRepository.findByName("GAME")).thenReturn(Optional.of(type(true)));
        when(categoryRepository.findByNameInAndActiveTrue(anyCollection())).thenReturn(List.of());
        when(downloader.download(anyString(), anyString())).thenAnswer(i ->
                new IgdbImageDownloader.DownloadedImage(new byte[]{1}, "image/jpeg", i.getArgument(0) + ".jpg"));
        when(productImageService.uploadImportedImage(any(), anyString(), anyString(), any(), anyInt()))
                .thenAnswer(i -> "s3/" + i.getArgument(4));
        service.importGame(44L);
        var ids = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(downloader, times(12)).download(ids.capture(), eq("t_1080p"));
        assertEquals(List.of("cover", "art1", "art2", "art3", "art4", "screen1", "screen2", "screen3",
                "screen4", "screen5", "screen6", "screen7"), ids.getAllValues());
        assertEquals(12, ids.getAllValues().stream().distinct().count());
        verify(productImageService, times(12)).uploadImportedImage(any(), eq("image/jpeg"), anyString(), any(), anyInt());
    }

    @Test void fillsTwelveWithAdditionalArtworksWhenScreenshotsAreUnavailable() {
        assertEquals(12, ProductImportService.selectImageIds("cover",
                java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(i -> "art" + i).toList(), List.of()).size());
    }

    @Test void fillsFromScreenshotsWhenArtworksAreUnavailableAndDeduplicatesIds() {
        List<String> selected = ProductImportService.selectImageIds("cover", List.of(),
                java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(i -> "shot" + i).toList());
        assertEquals(12, selected.size());
        assertEquals(12, selected.stream().distinct().count());
        assertEquals("cover", selected.getFirst());
    }

    @Test void usesAdditionalArtworksWhenFewerThanFourScreenshotsExist() {
        List<String> artworks = java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(i -> "art" + i).toList();
        List<String> selected = ProductImportService.selectImageIds("cover", artworks, List.of("shot1", "shot2"));
        assertEquals(12, selected.size());
        assertTrue(selected.containsAll(List.of("shot1", "shot2", "art5", "art6")));
    }

    @Test void reservesAtLeastFourScreenshotsAndDoesNotDuplicateAcrossImageTypes() {
        List<String> selected = ProductImportService.selectImageIds("same", List.of("same", "art1", "art2", "art3", "art4"),
                List.of("same", "shot1", "shot2", "shot3", "shot4", "shot5"));
        assertTrue(selected.containsAll(List.of("shot1", "shot2", "shot3", "shot4")));
        assertEquals(selected.size(), selected.stream().distinct().count());
        assertTrue(selected.size() <= 12);
    }

    @Test void compensatesUploadedObjectsWhenDatabaseCommitFails() {
        when(productRepository.existsByIgdbId(44L)).thenReturn(false);
        when(productRepository.existsBySlug("sample-game")).thenReturn(false);
        var metadata = new IgdbGetGameResponse(44L, "Sample Game", "A valid summary", null,
                "cover", List.of(), List.of(), List.of(), List.of(), List.of());
        when(igdbService.getGame(44L)).thenReturn(metadata);
        when(productTypeRepository.findByName("GAME")).thenReturn(Optional.of(type(true)));
        when(categoryRepository.findByNameInAndActiveTrue(anyCollection())).thenReturn(List.of());
        when(downloader.download("cover", "t_1080p"))
                .thenReturn(new IgdbImageDownloader.DownloadedImage(new byte[]{1}, "image/jpeg", "cover.jpg"));
        when(productImageService.uploadImportedImage(any(), anyString(), anyString(), any(), anyInt())).thenReturn("s3/cover");
        doThrow(new IllegalStateException("commit failed")).when(transactionManager).commit(any());
        assertThrows(IllegalStateException.class, () -> service.importGame(44L));
        verify(productImageService).deleteStoredObjects(List.of("s3/cover"));
    }

}
