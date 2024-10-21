package com.victorcov.worker.service;

import com.victorcov.worker.entity.Order;
import com.victorcov.worker.exceptions.ProductNotFoundException;
import com.victorcov.worker.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OrderService {
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final String RETRY_PREFIX = "order:retry:";
    private static final String ORDER_LOCK_PREFIX = "order:lock:";


    @Autowired
    private ProductService productService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;
    @Autowired
    private RedissonClient redissonClient;


    public void processOrder(Order order) {
        String lockKey = ORDER_LOCK_PREFIX + order.getOrderId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire the lock asynchronously with a wait and lease time
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                processOrderLogic(order)
                        .subscribe(
                                success -> log.info("Order processed successfully: {}", order.getOrderId()),
                                error -> log.error("Order processing failed for ID: {}. Error: {}", order.getOrderId(), error.getMessage())
                        );
            } else {
                log.warn("Could not acquire lock for order: {}", order.getOrderId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for order: {}", order.getOrderId());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Mono<Void> processOrderLogic(Order order) {
        return productService.enrichOrderProduct(order)
                .flatMap(validatedOrder -> {
                    if (validatedOrder.getProducts() == null || validatedOrder.getProducts().isEmpty()) {
                        log.warn("No products found for order: {}", validatedOrder.getOrderId());
                        return Mono.error(new ProductNotFoundException("No products found for order: " + validatedOrder.getOrderId()));
                    }
                    log.info("Product data enriched for order: {}", validatedOrder.getOrderId());
                    return clientService.validateClientInOrder(validatedOrder);
                })
                .flatMap(validatedOrder -> {
                    log.info("Client validated for order: {}", validatedOrder.getOrderId());
                    return orderRepository.save(validatedOrder)
                            .doOnSuccess(savedOrder ->
                                    log.info("Order successfully saved with ID: {}", savedOrder.getOrderId())
                            );
                })
                .then() // Converts Mono<Order> to Mono<Void>
                .doOnError(e -> log.error("Error processing order: {}. Error: {}", order.getOrderId(), e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Handling error for order ID: {}. Error: {}", order.getOrderId(), e.getMessage());
                    handleRetry(order);
                    return Mono.empty();
                });
    }

    private void handleRetry(Order order) {
        String retryKey = RETRY_PREFIX + order.getOrderId();
        ReactiveValueOperations<String, Object> ops = redisTemplate.opsForValue();

        ops.increment(retryKey)
                .flatMap(retries -> {
                    if (retries <= MAX_RETRY_ATTEMPTS) {
                        // Calcular el tiempo de espera exponencial basado en el número de reintentos
                        long backOff = (long) Math.pow(2, retries) * 1000; // 1s, 2s, 4s, etc.
                        return Mono.delay(Duration.ofMillis(backOff))
                                .then(reProcessOrder(order, retries)); // Reintenta el procesamiento del pedido
                    } else {
                        // Si se alcanzó el número máximo de reintentos, marca el pedido como fallido
                        return markOrderAsFailed(order);
                    }
                })
                .doOnError(e -> logError(order, e)) // Registra el error en caso de fallo
                .onErrorResume(e -> {
                    // Handle the error and prevent it from being dropped
                    log.error("Error processing retries for order: {}. Error: {}", order.getOrderId(), e.getMessage());
                    return Mono.empty(); // Or return a default value if needed
                })
                .subscribe();
    }

    private Mono<Void> markOrderAsFailed(Order order) {
        // Lógica para marcar el pedido como fallido (ej. guardarlo en un log o base de datos)
        log.error("Max retry attempts ({}) reached for order: {}",MAX_RETRY_ATTEMPTS, order.getOrderId());
        return Mono.empty();
    }

    private void logError(Order order, Throwable e) {
        // Lógica para registrar el error
        log.error("Error processing order: {}, Error: {}", order.getOrderId(), e.getMessage());
    }

     private Mono<Void> reProcessOrder(Order order, Long retries) {
         log.info("Retrying order: {}, time:{}", order.getOrderId(), retries);
         return processOrderLogic(order); // Call processOrderLogic for reprocessing
    }
}