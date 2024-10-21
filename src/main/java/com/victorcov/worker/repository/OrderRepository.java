package com.victorcov.worker.repository;

import com.victorcov.worker.entity.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends ReactiveMongoRepository<Order, String> {
}