package com.nynaromanoff.customer_service.service;

import com.nynaromanoff.customer_service.dto.AddressResponse;
import com.nynaromanoff.customer_service.dto.CustomerRequest;
import com.nynaromanoff.customer_service.dto.CustomerResponse;
import com.nynaromanoff.customer_service.model.Customer;
import com.nynaromanoff.customer_service.repository.CustomerRepository;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.admin-realm}")
    private String adminRealm;

    @Value("${keycloak.username}")
    private String adminUser;

    @Value("${keycloak.password}")
    private String adminPassword;

    @Value("${keycloak.client-id}")
    private String adminClientId;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info(
                "👤 [CustomerService] Iniciando cadastro para: {}",
                request.email()
        );

        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException(
                    "Este endereço de e-mail já está cadastrado."
            );
        }

        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase().trim())
                .cpf(request.cpf().replaceAll("[^0-9]", ""))
                .active(true)
                .addresses(
                        request.address() != null
                                ? new ArrayList<>(request.address())
                                : new ArrayList<>()
                )
                .build();

        String keycloakUserId = provisionarNoKeycloak(request);

        customer.setKeycloakUserId(keycloakUserId);

        customer = customerRepository.saveAndFlush(customer);

        log.info(
                "💾 [Postgres] Cliente persistido. ID={}, KeycloakID={}",
                customer.getId(),
                customer.getKeycloakUserId()
        );

        return toResponse(customer);
    }

    private CustomerResponse toResponse(Customer customer) {

        List<AddressResponse> addresses =
                customer.getAddresses()
                        .stream()
                        .map(addr -> new AddressResponse(
                                addr.getZipCode(),
                                addr.getStreet(),
                                addr.getNumber(),
                                addr.getComplement(),
                                addr.getNeighborhood(),
                                addr.getCity(),
                                addr.getState()
                        ))
                        .toList();

        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getActive(),
                addresses
        );
    }

    private String provisionarNoKeycloak(CustomerRequest request) {
        log.info("🛡️ [Keycloak] Comunicando com a API Admin para registrar credencial...");

        Keycloak kcAdmin = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .username(adminUser)
                .password(adminPassword)
                .clientId(adminClientId)
                .build();


        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(request.email().toLowerCase().trim());
        user.setEmail(request.email().toLowerCase().trim());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setCredentials(Collections.singletonList(credential));
        user.setEmailVerified(true);


        try (Response response = kcAdmin.realm(realm).users().create(user)) {
            if (response.getStatus() == 201) {
                String location = response.getHeaderString("Location");

                if (location == null || location.isBlank()) {
                    throw new RuntimeException("Keycloak criou o usuário, mas não retornou o Location.");
                }
                String keycloakUserId = location.substring(location.lastIndexOf("/") + 1);
                log.info("✅ [Keycloak] Usuário criado com ID: {}", keycloakUserId);
                return keycloakUserId;

            } else if (response.getStatus() == 409) {
                log.warn("⚠️ [Keycloak] Conflito: Usuário já existe no provedor de identidades.");
                throw new IllegalArgumentException("Este e-mail já está registrado no servidor de autenticação.");
            } else {
                log.error("❌ [Keycloak] Erro desconhecido ao provisionar. Status: {}", response.getStatus());
                throw new RuntimeException("Falha ao integrar credenciais com o servidor de identidade.");
            }
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(java.util.UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não localizado."));
        
        List<AddressResponse> listaDeDtos = customer.getAddresses().stream()
                .map(addr -> new AddressResponse(
                        addr.getZipCode(),
                        addr.getStreet(),
                        addr.getNumber(),
                        addr.getComplement(),
                        addr.getNeighborhood(),
                        addr.getCity(),
                        addr.getState()
                ))
                .toList();
        return new CustomerResponse(customer.getId(), customer.getFirstName(), customer.getLastName(), customer.getEmail(), customer.getActive(), listaDeDtos);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findCurrentCustomer(Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        log.info(
                "🔐 Buscando cliente autenticado pelo Keycloak ID: {}",
                keycloakUserId
        );
        Customer customer = customerRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cliente não localizado para o usuário autenticado."
                        )
                );
        return toResponse(customer);
    }
}
