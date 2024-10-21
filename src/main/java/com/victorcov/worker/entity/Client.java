package com.victorcov.worker.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
public class Client {
    private String customerId;
    private String name;
    private String email;
    private boolean isActive;
}
