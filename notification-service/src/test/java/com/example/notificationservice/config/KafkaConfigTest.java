package com.example.notificationservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class KafkaConfigTest {

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Test
    void errorHandlerCreatesDefaultErrorHandler() {
        KafkaConfig config = new KafkaConfig();

        DefaultErrorHandler errorHandler = config.errorHandler(kafkaTemplate);

        assertThat(errorHandler).isNotNull();
    }
}
