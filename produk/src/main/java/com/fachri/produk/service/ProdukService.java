package com.fachri.produk.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fachri.produk.model.Produk;
import com.fachri.produk.repository.ProdukRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@Service
public class ProdukService {
    @Autowired
    private ProdukRepository produkRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    // Metrik custom: menghitung berapa kali produk baru dibuat.
    // Ini yang nanti "ditarik" (scrape) oleh Prometheus lewat
    // endpoint /actuator/prometheus, lalu digambar jadi grafik di Grafana.
    private Counter produkDibuatCounter;

    @PostConstruct
    public void initMetrics() {
        produkDibuatCounter = Counter.builder("microservices_produk_dibuat_total")
                .description("Jumlah produk baru yang berhasil dibuat")
                .register(meterRegistry);
    }

    public List<Produk> getAllProduks(){
        return produkRepository.findAll();
    }

    public Produk getProdukById(Long id){
        return produkRepository.findById(id).orElse(null);
    }

    public Produk createProduk(Produk produk){
        Produk saved = produkRepository.save(produk);
        produkDibuatCounter.increment();
        return saved;
    }

    public void deleteProduk(Long id){
        produkRepository.deleteById(id);
    }

    public void deleteAll() {
    produkRepository.deleteAll();
    }
}
