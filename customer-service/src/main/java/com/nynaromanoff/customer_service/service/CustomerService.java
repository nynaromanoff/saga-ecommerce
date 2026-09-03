package com.nynaromanoff.customer_service.service;

import com.nynaromanoff.customer_service.dto.CustomerRequest;
import com.nynaromanoff.customer_service.dto.CustomerResponse;
import com.nynaromanoff.customer_service.model.Customer;
import com.nynaromanoff.customer_service.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.UUID;

@Service
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        log.info("👤 [CustomerService] Iniciando processo de cadastro para o e-mail: {}", request.email());

        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            log.warn("⚠️ Falha ao cadastrar: O e-mail {} já está sendo utilizado.", request.email());
            throw new IllegalArgumentException("Este endereço de e-mail já está cadastrado no sistema.");
        }

        if (customerRepository.existsByCpf(request.cpf())) {
            log.warn("⚠️ Falha ao cadastrar: O CPF {} já está sendo utilizado.", request.cpf());
            throw new IllegalArgumentException("Este documento CPF já está cadastrado no sistema.");
        }

        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase().trim())
                .cpf(request.cpf().replaceAll("[^0-8]", ""))
                .phone(request.phone())
                .active(true) // Todo cliente nasce ativo no ecossistema por padrão
                .addresses(new ArrayList<>())
                .build();

        customerRepository.save(customer);
        log.info("💾 [Postgres] Cliente cadastrado com sucesso sob o UUID: {}", customer.getId());


        return toResponse(customer);
    }
    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        log.info("🔍 [CustomerService] Buscando cadastro do cliente UUID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("❌ Cliente não localizado para o UUID: {}", id);
                    return new IllegalArgumentException("Cliente informado não foi localizado no sistema.");
                });

        return toResponse(customer);
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getCpf(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getActive()
        );
    }
}
