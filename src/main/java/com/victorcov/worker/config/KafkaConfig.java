package com.victorcov.worker.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        // Kafka consumer configuration settings
        Map<String, Object> props = new HashMap<>();

        // Define Kafka broker address
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        // Define consumer group ID
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order_group");

        // Enable auto-commit of offsets (can be set to false for manual commit)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Maximum records to fetch in a single poll
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);

        // Set the offset reset to earliest, in case there are no committed offsets
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Set the deserializers for keys and values
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Set up the error handler for retries
        factory.setCommonErrorHandler(commonErrorHandler());

        // Set up manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    @Bean
    public CommonErrorHandler commonErrorHandler() {
        // Create an exponential back-off for retries
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);  // Initial interval: 1 second
        backOff.setMultiplier(2.0);         // Multiplier: 2x for each retry
        backOff.setMaxInterval(10000L);     // Max interval: 10 seconds

        // Set up the default error handler with the back-off policy
        return new DefaultErrorHandler(backOff);
    }
}