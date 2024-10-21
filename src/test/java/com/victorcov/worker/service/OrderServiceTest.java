package com.victorcov.worker.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import com.victorcov.worker.entity.Order;
import com.victorcov.worker.entity.Product;
import com.victorcov.worker.repository.OrderRepository;
import com.victorcov.worker.exceptions.ProductNotFoundException;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private ClientService clientService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;
    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OrderService orderService;

    private Order order;

    @BeforeEach
    public void setup() {
        order = Order.builder()
                .orderId("order-009")
                .customerId("customer-001")
                .products(List.of(Product.builder().productId("product-101").build(), Product.builder().productId("product-1002").build()))
                .build();

    }

    @Test
    public void testProcessOrder_LockAcquiredSuccessfully() throws InterruptedException {
        // Mock successful lock acquisition
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true); // Mock the thread holding the lock
        when(productService.enrichOrderProduct(order)).thenReturn(Mono.just(order));
        when(clientService.validateClientInOrder(order)).thenReturn(Mono.just(order));
        when(orderRepository.save(order)).thenReturn(Mono.just(order));

        // Call the processOrder method
        orderService.processOrder(order);

        // Verify that lock was acquired and processing occurred
        verify(lock, times(1)).tryLock(5, 10, TimeUnit.SECONDS);
        verify(productService, times(1)).enrichOrderProduct(order);
        verify(clientService, times(1)).validateClientInOrder(order);
        verify(orderRepository, times(1)).save(order);

        // Verify lock release
        verify(lock, times(1)).unlock();
    }

    @Test
    public void testProcessOrder_LockNotAcquired() throws InterruptedException {
        // Mock unsuccessful lock acquisition
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

        // Call the method
        orderService.processOrder(order);

        // Verify lock acquisition attempt but no processing
        verify(lock, times(1)).tryLock(5, 10, TimeUnit.SECONDS);
        verify(productService, never()).enrichOrderProduct(any(Order.class));
        verify(clientService, never()).validateClientInOrder(any(Order.class));
    }

    @Test
    public void testProcessOrder_InterruptedWhileAcquiringLock() throws InterruptedException {
        // Mock lock acquisition interruption
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(5, 10, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        // Call the method
        orderService.processOrder(order);

        // Verify thread interruption handling
        verify(lock, never()).unlock();
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testProcessOrderLogic_Successful() {
        when(productService.enrichOrderProduct(order)).thenReturn(Mono.just(order));
        when(clientService.validateClientInOrder(order)).thenReturn(Mono.just(order));
        when(orderRepository.save(order)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.processOrderLogic(order))
                .expectComplete()
                .verify();

        verify(productService, times(1)).enrichOrderProduct(order);
        verify(clientService, times(1)).validateClientInOrder(order);
        verify(orderRepository, times(1)).save(order);
    }


    @Test
    public void testProcessOrderLogic_ProductNotFound() {

        // Mock ReactiveRedisTemplate to return the mocked ReactiveValueOperations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Mock increment behavior to avoid NPE
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        // Mock product enrichment failure
        when(productService.enrichOrderProduct(order))
                .thenReturn(Mono.error(new ProductNotFoundException("Product not found")));

        // Run processOrderLogic and expect completion
        StepVerifier.create(orderService.processOrderLogic(order))
                .expectComplete()
                .verify();

        // Verify interactions
        verify(productService, times(2)).enrichOrderProduct(order);
        verify(clientService, never()).validateClientInOrder(any(Order.class));
        verify(orderRepository, never()).save(any(Order.class));

        // Verify that retry logic is called
        verify(valueOperations, times(1)).increment(anyString());
    }
}