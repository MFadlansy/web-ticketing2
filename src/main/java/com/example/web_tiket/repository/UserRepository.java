package com.example.web_tiket.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.web_tiket.model.User;

// Anotasi @Repository menandakan bahwa ini adalah komponen Spring untuk akses data.
@Repository
// UserRepository mewarisi JpaRepository, yang menyediakan operasi CRUD dasar (Create, Read, Update, Delete)
// untuk entitas User dengan tipe primary key Long.
public interface UserRepository extends JpaRepository<User, Long> {

    // Metode kustom untuk menemukan User berdasarkan username.
    // Spring Data JPA akan secara otomatis mengimplementasikan metode ini berdasarkan namanya.
    Optional<User> findByUsername(String username);

    // Metode kustom untuk menemukan User berdasarkan email.
    Optional<User> findByEmail(String email);
}
