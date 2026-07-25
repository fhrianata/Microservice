package com.fachri.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
public class ConsumerService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MeterRegistry meterRegistry;

    @Value("${app.mail.to:fachrianata123@gmail.com}")
    private String mailTo;

    // Dua metrik custom: pesan yang diterima dari queue, dan email yang
    // berhasil dikirim setelahnya. Dipisah supaya kalau nanti email gagal
    // terkirim (SMTP down misalnya), kita bisa lihat gap-nya di Grafana:
    // pesan_diterima naik tapi email_terkirim tidak ikut naik.
    private Counter pesanDiterimaCounter;
    private Counter emailTerkirimCounter;

    @PostConstruct
    public void initMetrics() {
        pesanDiterimaCounter = Counter.builder("microservices_pesan_diterima_total")
                .description("Jumlah pesan yang diterima consumer dari RabbitMQ (orderQueue)")
                .register(meterRegistry);

        emailTerkirimCounter = Counter.builder("microservices_email_terkirim_total")
                .description("Jumlah email notifikasi order yang berhasil dikirim")
                .register(meterRegistry);
    }

    @RabbitListener(queues = "orderQueue")
    public void receivedMessage(String message) {

        pesanDiterimaCounter.increment();
        log.info("Pesan diterima dari orderQueue -> {}", message);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(mailTo);
        mail.setSubject("Order Berhasil");
        String emailText = "Halo,\n\n" +
                "Pesanan Anda telah berhasil diproses.\n\n" +
                "Detail Pesanan:\n" +
                "---------------------------\n" +
                message + "\n" +
                "---------------------------\n\n" +
                "Terima kasih telah berbelanja.\n" +
                "Salam,\n" +
                "Tim Sistem Order";

        mail.setText(emailText);


        mailSender.send(mail);

        emailTerkirimCounter.increment();
        log.info("Email notifikasi order berhasil dikirim");
    }
}