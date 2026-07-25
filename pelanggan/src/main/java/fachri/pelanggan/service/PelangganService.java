package fachri.pelanggan.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fachri.pelanggan.model.Pelanggan;
import fachri.pelanggan.Repository.PelangganRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@Service
public class PelangganService {

    @Autowired
    private PelangganRepository pelangganRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    // Metrik custom: menghitung berapa kali pelanggan baru terdaftar.
    private Counter pelangganDibuatCounter;

    @PostConstruct
    public void initMetrics() {
        pelangganDibuatCounter = Counter.builder("microservices_pelanggan_dibuat_total")
                .description("Jumlah pelanggan baru yang berhasil didaftarkan")
                .register(meterRegistry);
    }

    public List<Pelanggan> getAllPelanggan(){
        return pelangganRepository.findAll();
    }

    public Pelanggan getPelangganById(Long id){
        return pelangganRepository.findById(id).orElse(null);
    }

    public Pelanggan createPelanggan(Pelanggan pelanggan){
        Pelanggan saved = pelangganRepository.save(pelanggan);
        pelangganDibuatCounter.increment();
        return saved;
    }

    public void deletePelanggan(Long id){
        pelangganRepository.deleteById(id);
    }
}