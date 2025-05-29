package com.example.web_tiket.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Anotasi @Entity menandakan bahwa kelas ini adalah entitas JPA yang akan dipetakan ke tabel database.
@Entity
// Anotasi @Table digunakan untuk menentukan nama tabel di database.
@Table(name = "users")
// Anotasi Lombok untuk secara otomatis membuat getter, setter, toString, equals, dan hashCode.
@Data
// Anotasi Lombok untuk secara otomatis membuat konstruktor tanpa argumen.
@NoArgsConstructor
// Anotasi Lombok untuk secara otomatis membuat konstruktor dengan semua argumen.
@AllArgsConstructor
public class User {
    // Anotasi @Id menandakan bahwa field ini adalah primary key.
    @Id
    // Anotasi @GeneratedValue digunakan untuk menentukan strategi pembuatan nilai primary key.
    // GenerationType.IDENTITY berarti database akan mengelola penomoran otomatis (auto-increment).
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Anotasi @Column digunakan untuk menentukan properti kolom di database.
    // unique = true memastikan bahwa username harus unik.
    @Column(nullable = false, unique = true)
    private String username;

    // Password akan disimpan dalam bentuk hash, jadi tidak perlu unique.
    @Column(nullable = false)
    private String password;

    // Email juga harus unik.
    @Column(nullable = false, unique = true)
    private String email;

    // Anotasi @Enumerated(EnumType.STRING) digunakan untuk memetakan enum Java ke kolom VARCHAR di database.
    // Nilai enum akan disimpan sebagai string (misal: "USER", "ADMIN").
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // Menggunakan enum Role yang akan kita buat selanjutnya
}
