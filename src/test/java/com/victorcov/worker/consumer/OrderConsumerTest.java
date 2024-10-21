package com.victorcov.worker.consumer;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.victorcov.worker.service.OrderService;
import com.victorcov.worker.entity.Order;
import com.victorcov.worker.exceptions.ClientNotFoundException;
import com.victorcov.worker.exceptions.ProductNotFoundException;

@ExtendWith(MockitoExtension.class)
public class OrderConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderConsumer orderConsumer;

    @Test
    public void testConsume_SuccessfulProcessing() {
        // Given: Example Kafka message
        String message = "{\"orderId\":\"order-009\",\"customerId\":\"customer-001\",\"products\":[{\"productId\":\"product-101\"},{\"productId\":\"product-1002\"}]}";

        // When: OrderConsumer receives a message
        orderConsumer.consume(message);

        // Then: Verify that orderService processes the parsed order
        verify(orderService, times(1)).processOrder(any(Order.class));
    }

    @Test
    public void testConsume_ClientNotFound() {
        // Given: Example Kafka message
        String message = "{\"orderId\":\"order-009\",\"customerId\":\"customer-001\",\"products\":[{\"productId\":\"product-101\"}]}";

        // Simulate client not found during processing
        doThrow(new ClientNotFoundException("Client not found"))
                .when(orderService).processOrder(any(Order.class));

        // When & Then: OrderConsumer tries to process the message and fails due to client not found
        assertThrows(ClientNotFoundException.class, () -> orderConsumer.consume(message));

        // Verify that orderService was called but failed
        verify(orderService, times(1)).processOrder(any(Order.class));
    }

    @Test
    public void testConsume_ProductNotFound() {
        // Given: Example Kafka message
        String message = "{\"orderId\":\"order-009\",\"customerId\":\"customer-001\",\"products\":[{\"productId\":\"product-101\"}]}";

        // Simulate product not found during processing
        doThrow(new ProductNotFoundException("Product not found"))
                .when(orderService).processOrder(any(Order.class));

        // When & Then: OrderConsumer tries to process the message and fails due to product not found
        assertThrows(ProductNotFoundException.class, () -> orderConsumer.consume(message));

        // Verify that orderService was called but failed
        verify(orderService, times(1)).processOrder(any(Order.class));
    }

    @Test
    public void testConsume_InvalidOrder() {
        // Given: Invalid Kafka message
        String invalidMessage = "{\"orderId\":\"\",\"customerId\":\"customer-001\",\"products\":[]}";

        // When: OrderConsumer tries to process the invalid message
        orderConsumer.consume(invalidMessage);

        // Then: Verify no processing is done
        verify(orderService, never()).processOrder(any(Order.class));
    }

    @Test
    public void testConsume_InvalidMessage() {
        // Given: Invalid Kafka message
        String invalidMessage = "{\"theBadMessage\":\"Very bad message\"}";

        // When: OrderConsumer tries to process the invalid message
        orderConsumer.consume(invalidMessage);

        // Then: Verify no processing is done
        verify(orderService, never()).processOrder(any(Order.class));
    }
}