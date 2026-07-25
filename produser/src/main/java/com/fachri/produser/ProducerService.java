package com.fachri.produser;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProducerService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    // Metrik custom: menghitung berapa pesan yang berhasil dikirim ke RabbitMQ.
    private Counter pesanTerkirimCounter;

    @PostConstruct
    public void initMetrics() {
        pesanTerkirimCounter = Counter.builder("microservices_pesan_terkirim_total")
                .description("Jumlah pesan yang dikirim producer ke RabbitMQ (orderQueue)")
                .register(meterRegistry);
    }

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend("orderQueue", message);
        pesanTerkirimCounter.increment();
        log.info("Pesan terkirim ke orderQueue -> {}", message);
    }
}
