package com.victorcov.worker.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import reactor.core.publisher.Mono;

@Configuration
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.host}")
    private String mongoHost;

    @Value("${spring.data.mongodb.port}")
    private String mongoPort;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.username}")
    private String username;

    @Value("${spring.data.mongodb.password}")
    private String password;

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        // Authenticate against the 'admin' database
        String connectionString = String.format("mongodb://%s:%s@%s:%s/%s?authSource=admin",
                username, password, mongoHost, mongoPort, databaseName);
        return MongoClients.create(connectionString);
    }

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(MongoClient mongoClient) {
        return new ReactiveMongoTemplate(mongoClient, getDatabaseName());
    }

    @Bean
    public Mono<Void> initializeDatabase(ReactiveMongoTemplate reactiveMongoTemplate) {
        // Check if the 'orders' collection exists, if not, create it
        return reactiveMongoTemplate.collectionExists("orders")
                .flatMap(exists -> {
                    if (!exists) {
                        return reactiveMongoTemplate.createCollection("orders")
                                .then(createIndexes(reactiveMongoTemplate));
                    }
                    return Mono.empty();
                })
                .then(); // Return a Mono<Void> to indicate completion
    }

    private Mono<Void> createIndexes(ReactiveMongoTemplate reactiveMongoTemplate) {
        // Create an index on the 'orderId' field in the 'orders' collection
        ReactiveIndexOperations indexOps = reactiveMongoTemplate.indexOps("orders");
        Index orderIdIndex = new Index().on("orderId", Sort.Direction.ASC).unique();
        return indexOps.ensureIndex(orderIdIndex).then();
    }
}