package com.example.web_tiket.model;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated; // Untuk menyimpan tanggal saja
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String namaEvent; // Menggunakan camelCase untuk nama kolom di Java

    @Enumerated(EnumType.STRING) // Simpan ENUM sebagai String di DB
    @Column(nullable = false)
    private EventType tipeEvent; // Menggunakan enum EventType yang akan kita buat

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd") // Format untuk binding dari form HTML
    private LocalDate tanggal;

    @Column(nullable = false)
    private String lokasi;

    @Column(nullable = false)
    private Double hargaTiket; // Atau BigDecimal untuk presisi harga

    @Lob // Untuk kolom teks yang lebih panjang
    @Column(columnDefinition = "TEXT")
    private String deskripsi;

    // Kolom baru untuk gambar
    @Lob
    @Column(name = "gambar", columnDefinition="LONGBLOB", nullable = true)
    private byte[] gambar;

    // Enum untuk tipe event
    public enum EventType {
        KONSER,
        OLAHRAGA
    }
}