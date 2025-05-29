package com.example.web_tiket.service;

import java.io.IOException; // Sesuaikan dengan nama paket Anda
import java.util.List; // Sesuaikan dengan nama paket Anda
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.web_tiket.model.Event;
import com.example.web_tiket.repository.EventRepository;

@Service
public class EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // Metode untuk menyimpan event baru (termasuk upload gambar)
    public Event saveEvent(Event event, MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            event.setGambar(file.getBytes()); // Simpan data biner gambar
        }
        return eventRepository.save(event);
    }

    // Metode untuk memperbarui event (termasuk optional upload gambar baru)
    // Ini adalah metode yang dipanggil dari controller.
    public Event updateEvent(Long id, Event updatedEvent, MultipartFile file) throws IOException {
        return eventRepository.findById(id).map(existingEvent -> {
            existingEvent.setNamaEvent(updatedEvent.getNamaEvent());
            existingEvent.setTipeEvent(updatedEvent.getTipeEvent());
            existingEvent.setTanggal(updatedEvent.getTanggal());
            existingEvent.setLokasi(updatedEvent.getLokasi());
            existingEvent.setHargaTiket(updatedEvent.getHargaTiket());
            existingEvent.setDeskripsi(updatedEvent.getDeskripsi());

            // Hanya perbarui gambar jika ada file baru yang diunggah
            if (file != null && !file.isEmpty()) {
                try {
                    existingEvent.setGambar(file.getBytes());
                } catch (IOException e) {
                    throw new RuntimeException("Gagal membaca file gambar", e);
                }
            }
            // Jika file kosong atau null, gambar lama akan tetap ada

            return eventRepository.save(existingEvent);
        }).orElse(null); // Atau throw exception jika event tidak ditemukan
    }

    // ... metode lainnya ...
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(id);
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }
}