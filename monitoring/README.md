# Monitoring (Prometheus + Grafana + ELK) — Proyek Microservices PBL

Dokumen ini menjelaskan setup monitoring untuk proyek ini. **Sekarang seluruh
8 service Spring Boot ikut berjalan di dalam Docker**, satu jaringan dengan
Prometheus/Grafana/ELK — jadi tidak perlu lagi menjalankan tiap service
secara manual (`mvn spring-boot:run`) sebelum monitoring bisa bekerja.

---

## 1. Ringkasan Arsitektur

```
                     ┌──────────────┐
                     │   Eureka     │ :8761
                     └──────▲───────┘
                            │ register
     ┌──────────────────────┼───────────────────────────────┐
     │                      │                                │
┌────┴─────┐  ┌──────────┐ ┌┴─────────┐ ┌──────────┐ ┌───────┴──┐ ┌──────────┐
│ gateway  │  │authservice│ │  produk  │ │pelanggan │ │  order   │ │ produser │
│  :9000   │  │  :8086   │ │  :8081   │ │  :8082   │ │  :8083   │ │  :8080   │
└──────────┘  └────┬─────┘ └──────────┘ └──────────┘ └────┬─────┘ └────┬─────┘
                    │                                       │  RabbitMQ  │
               ┌────┴────┐                            ┌─────┴───────────┴───┐
               │  MySQL  │                            │      consumer       │
               └─────────┘                            │       :8085         │
                                                        └──────────────────────┘

Semua service di atas expose /actuator/prometheus  ->  Prometheus :9090 -> Grafana :3000
Semua service di atas kirim log (TCP)              ->  Logstash :5000  -> Elasticsearch -> Kibana :5601 (opsional)
```

## 2. Cara Menjalankan

```bash
# Dari folder root proyek (yang ada docker-compose.yml):

# 1) Build + jalankan semua service + MySQL + RabbitMQ + Prometheus + Grafana
docker compose up -d --build

# 2) (Opsional) tambahkan ELK untuk pencarian log terpusat
docker compose --profile elk up -d --build
```

Build pertama kali butuh waktu (Maven meng-compile 8 service dari source di
dalam container) dan butuh koneksi internet untuk mengunduh dependency Maven
dan base image Docker. Build berikutnya jauh lebih cepat karena Docker
meng-cache layer-nya.

Tunggu 1-2 menit sampai semua container `healthy` (cek dengan `docker compose ps`).

## 3. Verifikasi

- **Eureka** → `http://localhost:8761` — pastikan ke-8 service muncul di
  daftar "Instances currently registered with Eureka": `API-GATEWAY`,
  `AUTHSERVICE`, `PRODUK`, `PELANGGAN`, `ORDER`, `PRODUSER`, `CONSUMER`.
  (Eureka server sendiri tidak mendaftar ke dirinya sendiri, jadi wajar
  tidak ikut muncul di daftar itu.)
- **Prometheus** → `http://localhost:9090/targets` — pastikan semua job
  (`eureka`, `api-gateway`, `authservice`, `produk`, `pelanggan`, `order`,
  `produser`, `consumer`, `prometheus`) berstatus **UP**.
- **Grafana** → `http://localhost:3000` (login `admin`/`admin`, atau sesuai
  `.env`) — dashboard **"Microservices PBL - Overview"** otomatis muncul di
  folder **Microservices PBL** lewat provisioning.
- **RabbitMQ management UI** → `http://localhost:15672`
- **Kibana** (kalau pakai `--profile elk`) → `http://localhost:5601` — buat
  Data View baru dengan pattern `microservices-*` dan time field `@timestamp`.

## 4. Kalau Ada Target yang DOWN di Prometheus

1. `docker compose ps` — pastikan container-nya `running` dan `healthy`.
2. `docker compose logs -f <nama-service>` — lihat error startup (biasanya
   gagal connect ke Eureka/MySQL/RabbitMQ karena urutan start; `depends_on`
   dengan `condition: service_healthy` di `docker-compose.yml` seharusnya
   sudah menangani ini, tapi restart container yang bermasalah dengan
   `docker compose restart <service>` biasanya cukup).
3. Cek langsung endpoint metrik service tsb dari dalam network Docker:
   `docker compose exec prometheus wget -qO- http://<service>:<port>/actuator/prometheus`

## 5. Panel Dashboard yang Sudah Disiapkan

| Panel | Query PromQL | Menunjukkan |
|---|---|---|
| Service Up/Down | `up{job=~"eureka\|api-gateway\|authservice\|produk\|pelanggan\|order\|produser\|consumer"}` | Servis mana saja yang hidup |
| JVM Heap Used | `jvm_memory_used_bytes{area="heap"}` | Pemakaian memori tiap service |
| HTTP Request Rate | `rate(http_server_requests_seconds_count[1m])` | Trafik request per service |
| HTTP 5xx Error Rate | filter `status=~"5.."` | Error server per service |
| CPU Usage | `process_cpu_usage` | Beban CPU tiap service |
| Produk & Pelanggan Dibuat | `rate(microservices_produk_dibuat_total[1m])`, dst | Aktivitas bisnis: berapa data baru/menit |
| Order Dibuat per Role | `sum by (role) (rate(microservices_order_dibuat_total[1m]))` | Order dari ADMIN vs USER |
| Alur Pesan RabbitMQ | `rate(microservices_pesan_terkirim_total[1m])` vs `rate(microservices_pesan_diterima_total[1m])` vs `rate(microservices_email_terkirim_total[1m])` | Kesehatan pipeline: apakah semua pesan yang dikirim producer benar-benar sampai diproses & emailnya terkirim |
| Login Sukses vs Gagal | `sum by (hasil) (rate(microservices_auth_login_total[1m]))` | Deteksi dini percobaan login mencurigakan |
| Total Order per Role (pie) | `sum by (role) (microservices_order_dibuat_total)` | Distribusi order keseluruhan |

## 6. Kalau Mau Menjalankan Sebagian Service Saja di Luar Docker

Semua `application.properties`/`.yml` tetap punya default `localhost` untuk
tiap variabel (`EUREKA_URI`, `SPRING_DATASOURCE_URL`,
`SPRING_RABBITMQ_HOST`, dst), jadi kalau suatu saat Anda tetap ingin
menjalankan satu service tertentu langsung dari IDE (misalnya untuk
debugging), itu tetap bisa — servicenya akan otomatis fallback ke
`localhost` untuk semua dependency-nya, sama seperti sebelumnya.
