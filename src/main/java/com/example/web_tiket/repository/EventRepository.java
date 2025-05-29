package com.example.web_tiket.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.web_tiket.model.Event;

// EventRepository mewarisi JpaRepository untuk operasi CRUD dasar pada entitas Event.
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    // Spring Data JPA akan otomatis mengimplementasikan metode ini
}