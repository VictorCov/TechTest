package com.victorcov.worker.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Builder
@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String orderId;
    private String customerId;
    private List<Product> products;
}

