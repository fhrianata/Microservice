package com.fachri.authservice.service;

import com.fachri.authservice.dto.*;
import com.fachri.authservice.model.*;
import com.fachri.authservice.repository.UserRepository;
import com.fachri.authservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    // Metrik custom: register selalu dihitung sebagai 1 kategori,
    // sedangkan login dipisah "sukses" vs "gagal" -- supaya di Grafana
    // bisa kelihatan kalau tiba-tiba banyak percobaan login gagal
    // (indikasi ada yang coba brute-force password).
    private Counter registerCounter;
    private final Map<String, Counter> loginCounterPerHasil = new ConcurrentHashMap<>();

    @PostConstruct
    public void initMetrics() {
        registerCounter = Counter.builder("microservices_auth_register_total")
                .description("Jumlah user baru yang berhasil register")
                .register(meterRegistry);
    }

    public String register(RegisterRequest request) {

        User user = new User();
        user.setUsername(request.username);
        user.setEmail(request.email);
        user.setPassword(request.password); // nanti bisa hash
        user.setFullName(request.fullName);
        user.setRole(Role.USER);

        userRepository.save(user);

        registerCounter.increment();
        log.info("User baru register -> username={}", user.getUsername());

        return "Register berhasil";
    }

    public String login(LoginRequest request) {

        User user = userRepository.findByUsername(request.username)
                .orElseThrow(() -> {
                    catatLogin("gagal");
                    log.warn("Login gagal -> username={} (user tidak ditemukan)", request.username);
                    return new RuntimeException("User tidak ditemukan");
                });

        if (!user.getPassword().equals(request.password)) {
            catatLogin("gagal");
            log.warn("Login gagal -> username={} (password salah)", request.username);
            throw new RuntimeException("Password salah");
        }

        catatLogin("sukses");
        log.info("Login sukses -> username={}", request.username);

        return JwtUtil.generateToken(user.getUsername(), user.getRole().name());
    }

    private void catatLogin(String hasil) {
        loginCounterPerHasil
                .computeIfAbsent(hasil, h -> Counter.builder("microservices_auth_login_total")
                        .description("Jumlah percobaan login, dipisah sukses vs gagal")
                        .tags(Tags.of("hasil", h))
                        .register(meterRegistry))
                .increment();
    }
}