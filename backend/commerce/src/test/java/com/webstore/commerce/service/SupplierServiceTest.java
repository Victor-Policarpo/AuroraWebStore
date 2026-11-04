package com.webstore.commerce.service;

import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.Supplier;
import com.webstore.commerce.exception.errors.SupplierConfigurationException;
import com.webstore.commerce.exception.errors.SupplierCommunicationException;
import com.webstore.commerce.integration.suppliers.client.SupplierClient;
import com.webstore.commerce.integration.suppliers.dto.SupplierOfferResponse;
import com.webstore.commerce.repository.ProductRepository;
import com.webstore.commerce.repository.SupplierRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private SupplierOfferPersistenceService persistenceService;
    @Mock private SupplierClient client;

    private SupplierService service;

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @BeforeEach
    void setUp() {
        when(client.supplierCode()).thenReturn("MOCK_A");
        service = service(List.of(client));
    }

    @Test
    void synchronizesValidOfferUsingSteamAppIdMatch() {
        Product product = product(570L);
        Supplier supplier = supplier("MOCK_A");
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of(supplier));
        when(productRepository.findAllBySteamAppIdIsNotNull()).thenReturn(List.of(product));
        when(client.fetchOffers()).thenReturn(List.of(new SupplierOfferResponse("570", 4500L, "BRL", 12)));
        when(persistenceService.persistOffers(supplier.getId(), Map.of("570", product.getId()),
                List.of(new SupplierOfferResponse("570", 4500L, "BRL", 12)))).thenReturn(1);

        SupplierSyncResult result = service.synchronizeOffers();

        assertEquals(1, result.offersSaved());
        assertTrue(result.failures().isEmpty());
        verify(persistenceService).persistOffers(supplier.getId(), Map.of("570", product.getId()),
                List.of(new SupplierOfferResponse("570", 4500L, "BRL", 12)));
    }

    @Test
    void validatesRequiredOfferFieldsAndRanges() {
        List<SupplierOfferResponse> invalidResponses = List.of(
                new SupplierOfferResponse(null, 1L, "BRL", 1),
                new SupplierOfferResponse(" ", 1L, "BRL", 1),
                new SupplierOfferResponse("1", null, "BRL", 1),
                new SupplierOfferResponse("1", -1L, "BRL", 1),
                new SupplierOfferResponse("1", 1L, null, 1),
                new SupplierOfferResponse("1", 1L, "US", 1),
                new SupplierOfferResponse("1", 1L, "CAD", 1),
                new SupplierOfferResponse("1", 1L, "BRL", null),
                new SupplierOfferResponse("1", 1L, "BRL", -1)
        );

        for (SupplierOfferResponse response : invalidResponses) {
            assertFalse(VALIDATOR.validate(response).isEmpty(), "Expected validation error for " + response);
        }
        assertTrue(VALIDATOR.validate(new SupplierOfferResponse("1", 0L, "BRL", 0)).isEmpty());
    }

    @Test
    void invalidResponseIsReportedAndNeverPersisted() {
        Supplier supplier = supplier("MOCK_A");
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of(supplier));
        when(productRepository.findAllBySteamAppIdIsNotNull()).thenReturn(List.of());
        when(client.fetchOffers()).thenReturn(List.of(new SupplierOfferResponse("570", -1L, "BRL", 8)));

        SupplierSyncResult result = service.synchronizeOffers();

        assertEquals(0, result.offersSaved());
        assertEquals(SupplierSyncFailure.Type.INVALID_RESPONSE, result.failures().getFirst().type());
        verifyNoInteractions(persistenceService);
    }

    @Test
    void missingClientForActiveSupplierIsAnExplicitConfigurationFailure() {
        Supplier supplier = supplier("UNCONFIGURED");
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of(supplier));
        service = service(List.of());

        assertThrows(SupplierConfigurationException.class, service::synchronizeOffers);

        verifyNoInteractions(productRepository, persistenceService);
    }

    @Test
    void doesNotQueryInactiveSuppliers() {
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of());
        when(productRepository.findAllBySteamAppIdIsNotNull()).thenReturn(List.of());

        SupplierSyncResult result = service.synchronizeOffers();

        assertEquals(0, result.offersSaved());
        assertTrue(result.failures().isEmpty());
        verify(client, never()).fetchOffers();
        verifyNoInteractions(persistenceService);
    }

    @Test
    void supplierFailurePreservesItsOffersAndDoesNotStopOtherSuppliers() {
        SupplierClient failedClient = mock(SupplierClient.class);
        when(failedClient.supplierCode()).thenReturn("MOCK_A");
        when(failedClient.fetchOffers()).thenThrow(new SupplierCommunicationException("network unavailable"));
        SupplierClient workingClient = mock(SupplierClient.class);
        when(workingClient.supplierCode()).thenReturn("MOCK_B");
        SupplierOfferResponse response = new SupplierOfferResponse("570", 5200L, "BRL", 3);
        when(workingClient.fetchOffers()).thenReturn(List.of(response));

        Supplier failedSupplier = supplier("MOCK_A");
        Supplier workingSupplier = supplier("MOCK_B");
        Product product = product(570L);
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of(failedSupplier, workingSupplier));
        when(productRepository.findAllBySteamAppIdIsNotNull()).thenReturn(List.of(product));
        when(persistenceService.persistOffers(workingSupplier.getId(), Map.of("570", product.getId()),
                List.of(response))).thenReturn(1);
        service = service(List.of(failedClient, workingClient));

        SupplierSyncResult result = service.synchronizeOffers();

        assertEquals(1, result.offersSaved());
        assertEquals(1, result.failures().size());
        assertEquals("MOCK_A", result.failures().getFirst().supplierCode());
        assertEquals(SupplierSyncFailure.Type.COMMUNICATION, result.failures().getFirst().type());
        verify(persistenceService, never()).persistOffers(eq(failedSupplier.getId()), anyMap(), anyList());
        verify(persistenceService).persistOffers(workingSupplier.getId(), Map.of("570", product.getId()),
                List.of(response));
    }

    @Test
    void unexpectedClientRuntimeExceptionIsNotTreatedAsCommunicationFailure() {
        Supplier supplier = supplier("MOCK_A");
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of(supplier));
        when(client.fetchOffers()).thenThrow(new IllegalStateException("programming error"));
        when(productRepository.findAllBySteamAppIdIsNotNull()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, service::synchronizeOffers);
        verifyNoInteractions(persistenceService);
    }

    @Test
    void ignoresOffersWithoutMatchingProduct() {
        Supplier supplier = supplier("MOCK_A");
        when(supplierRepository.findByActiveTrue()).thenReturn(List.of(supplier));
        when(productRepository.findAllBySteamAppIdIsNotNull()).thenReturn(List.of(product(1L)));
        when(client.fetchOffers()).thenReturn(List.of(new SupplierOfferResponse("999", 100L, "BRL", 2)));
        when(persistenceService.persistOffers(eq(supplier.getId()), anyMap(), anyList())).thenReturn(0);

        SupplierSyncResult result = service.synchronizeOffers();

        assertEquals(0, result.offersSaved());
    }

    private SupplierService service(List<SupplierClient> clients) {
        return new SupplierService(supplierRepository, productRepository, persistenceService, VALIDATOR, clients);
    }

    private Product product(long steamAppId) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSteamAppId(steamAppId);
        return product;
    }

    private Supplier supplier(String code) {
        Supplier supplier = new Supplier();
        supplier.setId(UUID.randomUUID());
        supplier.setCode(code);
        supplier.setActive(true);
        return supplier;
    }
}
