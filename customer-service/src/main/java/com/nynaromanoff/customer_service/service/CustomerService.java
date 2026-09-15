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
        log.info("👤 [CustomerService] Iniciando cadastro global para: {}", request.email());

        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("Este endereço de e-mail já está cadastrado.");
        }

        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase().trim())
                .cpf(request.cpf().replaceAll("[^0-9]", ""))
                .active(true)
                .build();

        // 3. 🔥 TRATAMENTO DOS ENDEREÇOS (Evita tabelas órfãs ou nulas)
        if (request.address() != null && !request.address().isEmpty()) {
            // Se você usa @ElementCollection ou @OneToMany, injetamos a lista tratada
            customer.setAddresses(new ArrayList<>(request.address()));
        } else {
            customer.setAddresses(new ArrayList<>());
        }

        // 4. 🔥 PERSISTÊNCIA BRUTA NO POSTGRES
        // O flush força o Hibernate a cuspir o INSERT na tabela IMEDIATAMENTE
        customer = customerRepository.saveAndFlush(customer);
        log.info("💾 [Postgres] Cliente persistido com sucesso sob o ID: {}", customer.getId());

        // 5. PROVISIONAMENTO NO KEYCLOAK (Deixamos por último!)
        // Se o Keycloak falhar, o @Transactional cancelará o insert do Postgres automaticamente
        provisionarNoKeycloak(request);

        List<AddressResponse> addressResponses = customer.getAddresses().stream()
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
                addressResponses
        );
    }

    private void provisionarNoKeycloak(CustomerRequest request) {
        log.info("🛡️ [Keycloak] Comunicando com a API Admin para registrar credencial...");

        Keycloak kcAdmin = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(adminRealm)
                .username(adminUser)
                .password(adminPassword)
                .clientId(adminClientId)
                .build();

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(request.email().toLowerCase().trim());
        user.setEmail(request.email().toLowerCase().trim());
        user.setFirstName(request.firstName());
        user.setEmailVerified(true);

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.password());
        user.setCredentials(Collections.singletonList(passwordCred));

        try (Response response = kcAdmin.realm(realm).users().create(user)) {
            if (response.getStatus() == 201) {
                log.info("✅ [Keycloak] Usuário provisionado e senha criptografada com sucesso!");
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
}
