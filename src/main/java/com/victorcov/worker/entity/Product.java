package com.victorcov.worker.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Product {
    private String productId;
    private String name;
    private double price;
}