package com.fintech.transaction_service.common.Kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void publishTransactionInitiated(TransactionEvent event) {
        log.info("Publishing transaction.initiated event: {}", event.getTransactionId());
        kafkaTemplate.send("transaction.initiated", event.getTransactionId(), event);
    }

    public void publishTransactionCompleted(TransactionEvent event) {
        log.info("Publishing transaction.completed event: {}", event.getTransactionId());
        kafkaTemplate.send("transaction.completed", event.getTransactionId(), event);
    }

    public void publishTransactionFailed(TransactionEvent event) {
        log.info("Publishing transaction.failed event: {}", event.getTransactionId());
        kafkaTemplate.send("transaction.failed", event.getTransactionId(), event);
    }
}