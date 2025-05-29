package com.example.web_tiket.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.security.core.context.SecurityContextHolder; // Import SecurityContextHolder
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.web_tiket.model.Event;
import com.example.web_tiket.model.Event.EventType;
import com.example.web_tiket.model.User; // Import User
import com.example.web_tiket.repository.UserRepository; // Import UserRepository
import com.example.web_tiket.service.EventService;

@Controller
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository; // Injeksi UserRepository

    @Autowired
    public EventController(EventService eventService, UserRepository userRepository) { // Tambahkan UserRepository di konstruktor
        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    // Helper method untuk menambahkan informasi user ke model
    private void addUserInfoToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
            String username = authentication.getName();
            Optional<User> userOptional = userRepository.findByUsername(username); // Mengambil user dari DB
            userOptional.ifPresent(user -> model.addAttribute("username", user.getUsername())); // Menambahkan username ke model
        }
    }

    // --- Endpoints untuk User (Melihat Event) ---

    // Menampilkan daftar semua event (bisa diakses public atau authenticated user)
    @GetMapping("/events")
    public String listEvents(Model model) {
        addUserInfoToModel(model); // Panggil helper method
        List<Event> events = eventService.getAllEvents();
        model.addAttribute("events", events);
        return "events"; // Mengembalikan nama view (events.html)
    }

    // Menampilkan detail event
    @GetMapping("/events/{id}")
    public String showEventDetail(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        addUserInfoToModel(model); // Panggil helper method
        Event event = eventService.getEventById(id).orElse(null);
        if (event == null) {
            redirectAttributes.addFlashAttribute("error", "Event tidak ditemukan.");
            return "redirect:/events";
        }
        model.addAttribute("event", event);
        return "event-detail"; // Mengembalikan nama view (event-detail.html)
    }

    // Endpoint untuk menampilkan gambar event dari database
    @GetMapping("/events/image/{id}")
    public ResponseEntity<byte[]> getEventImage(@PathVariable Long id) {
        return eventService.getEventById(id)
                .map(event -> {
                    byte[] imageData = event.getGambar();
                    if (imageData != null && imageData.length > 0) {
                        MediaType contentType = MediaType.IMAGE_JPEG; // Default
                        return ResponseEntity.ok()
                                .contentType(contentType)
                                .body(imageData);
                    } else {
                        return ResponseEntity.notFound().<byte[]>build();
                    }
                })
                .orElse(ResponseEntity.notFound().<byte[]>build());
    }


    // --- Endpoints untuk Admin (Manajemen Event) ---

    // Menampilkan halaman kelola event untuk admin
    @GetMapping("/admin/events")
    public String adminListEvents(Model model) {
        // Asumsi di halaman admin ini, user pasti sudah login sebagai admin, jadi tidak perlu tambahkan logika login/register.
        // Cukup tampilkan username admin.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
        }
        List<Event> events = eventService.getAllEvents();
        model.addAttribute("events", events);
        model.addAttribute("newEvent", new Event());
        model.addAttribute("eventTypes", EventType.values());
        return "admin/events";
    }

    // Memproses penambahan event baru oleh admin
    @PostMapping("/admin/events/add")
    public String addEvent(@ModelAttribute("newEvent") Event event,
                           @RequestParam("file") MultipartFile file,
                           RedirectAttributes redirectAttributes) {
        try {
            eventService.saveEvent(event, file);
            redirectAttributes.addFlashAttribute("message", "Event berhasil ditambahkan.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menambahkan event: " + e.getMessage());
        }
        return "redirect:/admin/events";
    }

    // Menampilkan form edit event untuk admin
    @GetMapping("/admin/events/edit/{id}")
    public String showEditEventForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        // Asumsi di halaman admin ini, user pasti sudah login sebagai admin.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
        }

        Event event = eventService.getEventById(id).orElse(null);
        if (event == null) {
            redirectAttributes.addFlashAttribute("error", "Event tidak ditemukan.");
            return "redirect:/admin/events";
        }
        model.addAttribute("event", event);
        model.addAttribute("eventTypes", EventType.values());
        return "admin/edit-event";
    }

    // Memproses pembaruan event oleh admin
    @PostMapping("/admin/events/update/{id}")
    public String updateEvent(@PathVariable("id") Long id,
                              @ModelAttribute("event") Event event,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        event.setId(id);
        try {
            eventService.updateEvent(id, event, file);
            redirectAttributes.addFlashAttribute("message", "Event berhasil diperbarui.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal memperbarui event: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal memperbarui event: " + e.getMessage());
        }
        return "redirect:/admin/events";
    }

    // Memproses penghapusan event oleh admin
    @GetMapping("/admin/events/delete/{id}")
    public String deleteEvent(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id);
            redirectAttributes.addFlashAttribute("message", "Event berhasil dihapus.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menghapus event: " + e.getMessage());
        }
        return "redirect:/admin/events";
    }
}