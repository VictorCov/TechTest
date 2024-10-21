package com.victorcov.worker.service;

import com.victorcov.worker.entity.Order;
import com.victorcov.worker.entity.Product;
import com.victorcov.worker.exceptions.ProductNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class ProductService {
    private final WebClient webClient = WebClient.create("http://localhost:8081/products");


    public Mono<Order> enrichOrderProduct(Order order) {

        List<String> requestedProductIds = order.getProducts()
                .stream()
                .map(Product::getProductId)
                .toList();

        return webClient.get()
                .uri("/by-ids?ids=" + String.join(",",
                        order.getProducts()
                                .stream()
                                .map(Product::getProductId)
                                .toList()))
                .exchangeToFlux(response ->
                        response.bodyToFlux(Product.class)
                )
                .doOnSubscribe(subscription ->
                        log.info("Sending request to Product API for order: {}", order.getOrderId())
                )
                .doOnNext(product ->
                        log.info("Received product data: {} - {}", product.getProductId(), product.getName())
                )
                .collectList()
                .flatMap(products -> {
                    // Check if any products were returned
                    if (products.isEmpty()) {
                        log.warn("No products found for order: {}", order.getOrderId());
                        return Mono.error(new ProductNotFoundException("Products not found for order: " + order.getOrderId()));
                    }
                    // Extract returned product IDs
                    List<String> returnedProductIds = products.stream()
                            .map(Product::getProductId)
                            .toList();

                    // Find missing product IDs
                    List<String> missingProductIds = requestedProductIds.stream()
                            .filter(id -> !returnedProductIds.contains(id))
                            .toList();

                    // Log warning for missing products
                    if (!missingProductIds.isEmpty()) {
                        log.warn("Products not found for order {}: {}. Will be ignored", order.getOrderId(), missingProductIds);
                    }
                    // Log before setting the products in the order
                    log.info("Setting enriched products in the order: {}", order.getOrderId());
                    order.setProducts(products);
                    return Mono.just(order);
                });

    }
}