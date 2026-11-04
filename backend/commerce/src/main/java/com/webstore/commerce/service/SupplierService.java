package com.webstore.commerce.service;

import com.webstore.commerce.entity.Product;
import com.webstore.commerce.entity.Supplier;
import com.webstore.commerce.exception.errors.SupplierConfigurationException;
import com.webstore.commerce.exception.errors.SupplierCommunicationException;
import com.webstore.commerce.integration.suppliers.client.SupplierClient;
import com.webstore.commerce.integration.suppliers.dto.SupplierOfferResponse;
import com.webstore.commerce.repository.ProductRepository;
import com.webstore.commerce.repository.SupplierRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);

    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final SupplierOfferPersistenceService persistenceService;
    private final Validator validator;
    private final Map<String, SupplierClient> clientsByCode;

    public SupplierService(
            SupplierRepository supplierRepository,
            ProductRepository productRepository,
            SupplierOfferPersistenceService persistenceService,
            Validator validator,
            List<SupplierClient> supplierClients
    ) {
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.persistenceService = persistenceService;
        this.validator = validator;
        this.clientsByCode = supplierClients.stream().collect(Collectors.toUnmodifiableMap(
                SupplierClient::supplierCode, Function.identity()));
    }

    public SupplierSyncResult synchronizeOffers() {
        List<Supplier> activeSuppliers = supplierRepository.findByActiveTrue();
        validateClientConfiguration(activeSuppliers);

        Map<String, UUID> productIdsBySteamAppId = productRepository.findAllBySteamAppIdIsNotNull()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        product -> String.valueOf(product.getSteamAppId()), Product::getId));

        List<SupplierSyncFailure> failures = new ArrayList<>();
        int savedOffers = 0;

        for (Supplier supplier : activeSuppliers) {
            SupplierClient client = clientsByCode.get(supplier.getCode());
            List<SupplierOfferResponse> responses;
            try {
                responses = client.fetchOffers();
            } catch (SupplierCommunicationException exception) {
                log.warn("Unable to synchronize offers from supplier {}. Existing offers were preserved.",
                        supplier.getCode(), exception);
                failures.add(new SupplierSyncFailure(supplier.getCode(),
                        SupplierSyncFailure.Type.COMMUNICATION, "Supplier offer consultation failed"));
                continue;
            }

            List<String> validationErrors = validateResponses(responses);
            if (!validationErrors.isEmpty()) {
                log.warn("Supplier {} returned invalid offer data: {}", supplier.getCode(), validationErrors);
                failures.add(new SupplierSyncFailure(supplier.getCode(),
                        SupplierSyncFailure.Type.INVALID_RESPONSE, String.join("; ", validationErrors)));
                continue;
            }

            savedOffers += persistenceService.persistOffers(
                    supplier.getId(), productIdsBySteamAppId, responses);
        }

        return new SupplierSyncResult(savedOffers, failures);
    }

    private void validateClientConfiguration(List<Supplier> suppliers) {
        List<String> missingClients = suppliers.stream()
                .map(Supplier::getCode)
                .filter(code -> !clientsByCode.containsKey(code))
                .toList();
        if (!missingClients.isEmpty()) {
            throw new SupplierConfigurationException(
                    "No SupplierClient configured for active supplier codes: " + String.join(", ", missingClients));
        }
    }

    private List<String> validateResponses(List<SupplierOfferResponse> responses) {
        if (responses == null) {
            return List.of("Offer response list must not be null");
        }

        List<String> errors = new ArrayList<>();
        for (int index = 0; index < responses.size(); index++) {
            SupplierOfferResponse response = responses.get(index);
            if (response == null) {
                errors.add("Offer at index " + index + " must not be null");
                continue;
            }

            for (ConstraintViolation<SupplierOfferResponse> violation : validator.validate(response)) {
                errors.add("Offer at index " + index + ": " + violation.getPropertyPath()
                        + " " + violation.getMessage());
            }
        }
        return errors;
    }
}
