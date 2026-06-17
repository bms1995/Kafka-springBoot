package com.example.notificationservice.config;

import com.example.commonkafka.KafkaErrorHandlers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Slf4j
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        return KafkaErrorHandlers.deadLetterErrorHandler(kafkaTemplate, log);
    }
}
