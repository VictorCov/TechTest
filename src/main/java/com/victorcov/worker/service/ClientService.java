package com.victorcov.worker.service;

import com.victorcov.worker.entity.Client;
import com.victorcov.worker.entity.Order;
import com.victorcov.worker.exceptions.ClientInactiveException;
import com.victorcov.worker.exceptions.ClientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ClientService {
    private final WebClient webClient = WebClient.create("http://localhost:8082/clients");


    public Mono<Order> validateClientInOrder(Order order) {
        String customerId = order.getCustomerId();

        return webClient.get()
                .uri("/{id}", customerId)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError()) {
                        log.warn("Client not found with ID: {}", customerId);
                        return Mono.error(new ClientNotFoundException("Client not found with ID: " + customerId));
                    }
                    return response.bodyToMono(Client.class)
                            .flatMap(client -> {
                                if (!client.isActive()) {
                                    log.warn("Client with ID: {} is inactive", customerId);
                                    return Mono.error(new ClientInactiveException("Client with ID: " + customerId + " is inactive"));
                                }
                                log.info("Client found and active for ID: {}", customerId);
                                return Mono.empty(); // No body needed, just checking existence
                            });
                })
                .doOnSubscribe(subscription ->
                        log.info("Sending request to Client API for client ID: {}", customerId)
                )
                .doOnSuccess(unused ->
                        log.info("Client found and validated for ID: {}", customerId)
                )
                .doOnError(e ->
                        log.info("Error validating client ID: {}. Error: {}", customerId, e.getMessage())
                )
                .onErrorResume(e -> {
                    // Log and handle the error by returning a default response or error
                    log.error("Error processing client ID: {}. Error: {}", customerId, e.getMessage());
                    return Mono.error(e); // Rethrow the error or provide a default response
                })
                .thenReturn(order); // Return the order if the client exists
    }
}