package com.victorcov.worker.consumer;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.victorcov.worker.entity.Order;
import com.victorcov.worker.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderConsumer {
    private final Gson gson = new Gson();
    @Autowired
    private OrderService orderService;

    @KafkaListener(topics = "orders_topic", groupId = "order_group")
    public void consume(String message) {
        Order order = parseMessage(message);
        if (order != null && isValidOrder(order)) {
            orderService.processOrder(order);
        } else {
            // Log discarded message due to invalid JSON format
            log.warn("Discarded invalid message: {}", message);
        }
    }

    private Order parseMessage(String message) {
        try {
            // Deserializa el mensaje JSON a un objeto Order
            return gson.fromJson(message, Order.class);
        } catch (JsonSyntaxException e) {
            // Manejo de error si el mensaje no tiene un formato JSON válido
            throw new IllegalArgumentException("Invalid JSON format for Order: " + message, e);
        }
    }

    private boolean isValidOrder(Order order) {
        return order.getOrderId() != null
                && order.getCustomerId() != null
                && !order.getCustomerId().isEmpty()
                && order.getProducts() != null
                && !order.getProducts().isEmpty()
                && order.getProducts().stream().allMatch(product -> product.getProductId() != null && !product.getProductId().isEmpty());
    }

}