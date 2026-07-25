package fachri.order.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import fachri.order.model.Order;
import fachri.order.repository.OrderRepository;
import fachri.order.vo.Produk;
import fachri.order.vo.ResponseTemplate;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import jakarta.transaction.Transactional;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    // Metrik custom: menghitung jumlah order yang dibuat, dipisah per role
    // (contoh: ADMIN vs USER) -- polanya sama seperti Counter per kategori
    // di proyek BeanSense (klasifikasi warna biji kopi).
    private final Map<String, Counter> counterPerRole = new ConcurrentHashMap<>();

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    public Order createOrder(Order order, String username, String role) {

        order.setCreatedBy(username);
        order.setRole(role);

        Order saved = orderRepository.save(order);

        String message = "Order ID: " + saved.getId() +
                ", Total: " + saved.getTotal() +
                ", User: " + username +
                ", Role: " + role;

        rabbitTemplate.convertAndSend("orderQueue", message);

        counterPerRole
                .computeIfAbsent(role, r -> Counter.builder("microservices_order_dibuat_total")
                        .description("Jumlah order yang dibuat, per role user")
                        .tags(Tags.of("role", r))
                        .register(meterRegistry))
                .increment();

        log.info("Order dibuat -> order_id={}, user={}, role={}", saved.getId(), username, role);

        return saved;
    }

    @Transactional
    public void update(Long orderId, Integer jumlah, String tanggal, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order tidak ada"));

        if (jumlah != null) {
            order.setJumlah(jumlah);
        }
        if (tanggal != null && tanggal.length() > 0
                && !Objects.equals(order.getTanggal(), tanggal)) {
            order.setTanggal(tanggal);
        }
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<ResponseTemplate> getOrderWithProdukById(Long id) {
        List<ResponseTemplate> resoponseList = new ArrayList<>();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Order tidak ditemukan"));

        ServiceInstance serviceInstance = discoveryClient.getInstances("PRODUK").get(0);

        Produk produk = restTemplate.getForObject(
                serviceInstance.getUri() + "/api/produk/" + order.getProdukId(),
                Produk.class);

        ResponseTemplate vo = new ResponseTemplate();
        vo.setOrder(order);
        vo.setProduk(produk);

        resoponseList.add(vo);
        return resoponseList;
    }

    public void deleteOrder(long id) {
        orderRepository.deleteById(id);
    }
}