package com.example.web_tiket.controller;

import java.util.List;
import java.util.Optional; // Import Optional

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.web_tiket.model.Role;
import com.example.web_tiket.model.User;
import com.example.web_tiket.service.UserService;

// Anotasi @Controller menandakan bahwa kelas ini adalah komponen controller Spring MVC.
@Controller
public class UserController {

    // Injeksi dependensi UserService untuk mengakses logika bisnis terkait pengguna.
    private final UserService userService;

    // Konstruktor untuk injeksi dependensi.
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Helper method untuk mendapatkan user yang sedang login
    private Optional<User> getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        String username = authentication.getName();
        return userService.findByUsername(username); // Menggunakan userService untuk mencari user
    }


    // --- Endpoints untuk Registrasi dan Login ---

    // Menampilkan halaman registrasi
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User()); // Menambahkan objek User kosong untuk binding form
        return "register"; // Mengembalikan nama view (register.html)
    }

    // Memproses permintaan registrasi
    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        // Coba daftarkan pengguna baru
        User registeredUser = userService.registerUser(user);
        if (registeredUser != null) {
            redirectAttributes.addFlashAttribute("message", "Registrasi berhasil! Silakan login.");
            return "redirect:/login"; // Redirect ke halaman login setelah berhasil
        } else {
            redirectAttributes.addFlashAttribute("error", "Username atau Email sudah terdaftar.");
            return "redirect:/register"; // Kembali ke halaman registrasi jika gagal
        }
    }

    // Menampilkan halaman login
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", "Username atau password salah.");
        }
        if (logout != null) {
            model.addAttribute("message", "Anda telah berhasil logout.");
        }
        return "login"; // Mengembalikan nama view (login.html)
    }

    // Halaman dashboard setelah login (akan disesuaikan nanti)
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        model.addAttribute("username", username);
        model.addAttribute("roles", authentication.getAuthorities());
        return "dashboard"; // Mengembalikan nama view (dashboard.html)
    }

    // --- New Endpoints for User Profile ---

    // Menampilkan halaman profil pengguna yang sedang login
    @GetMapping("/profile")
    public String showProfile(Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pengguna tidak ditemukan. Silakan login ulang.");
            return "redirect:/login";
        }
        model.addAttribute("user", userOptional.get());
        return "profile"; // Mengembalikan nama view (profile.html)
    }

    // Memproses pembaruan profil pengguna yang sedang login
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOptional = getLoggedInUser();
        if (currentUserOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pengguna tidak ditemukan. Silakan login ulang.");
            return "redirect:/login";
        }

        User currentUser = currentUserOptional.get();
        // Pastikan ID user yang diperbarui adalah ID user yang sedang login
        if (!currentUser.getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Anda tidak memiliki izin untuk mengubah profil pengguna lain.");
            return "redirect:/profile";
        }

        // Set role kembali ke role user yang sedang login untuk mencegah perubahan role melalui form
        user.setRole(currentUser.getRole());

        User updated = userService.updateUser(user.getId(), user); // Menggunakan service untuk update
        if (updated != null) {
            redirectAttributes.addFlashAttribute("message", "Profil berhasil diperbarui.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal memperbarui profil. Mungkin email sudah terdaftar.");
        }
        return "redirect:/profile";
    }


    // --- Endpoints untuk CRUD Data User oleh Admin ---

    // Menampilkan daftar semua pengguna (hanya untuk ADMIN)
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("newUser", new User()); // Untuk form tambah user
        model.addAttribute("roles", Role.values()); // Untuk dropdown role
        return "admin/users"; // Mengembalikan nama view (admin/users.html)
    }

    // Menampilkan form edit user (hanya untuk ADMIN)
    @GetMapping("/admin/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Pengguna tidak ditemukan.");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "admin/edit-user"; // Mengembalikan nama view (admin/edit-user.html)
    }

    // Memproses penambahan user baru oleh admin
    @PostMapping("/admin/users/add")
    public String addUser(@ModelAttribute("newUser") User user, RedirectAttributes redirectAttributes) {
        // Password harus di-set dan dienkripsi di service
        User savedUser = userService.registerUser(user); // Menggunakan registerUser untuk enkripsi password
        if (savedUser != null) {
            redirectAttributes.addFlashAttribute("message", "Pengguna berhasil ditambahkan.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal menambahkan pengguna. Username atau email mungkin sudah ada.");
        }
        return "redirect:/admin/users";
    }

    // Memproses pembaruan data user oleh admin
    @PostMapping("/admin/users/update/{id}")
    public String updateUser(@PathVariable("id") Long id, @ModelAttribute("user") User user, RedirectAttributes redirectAttributes) {
        User updated = userService.updateUser(id, user);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("message", "Pengguna berhasil diperbarui.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal memperbarui pengguna.");
        }
        return "redirect:/admin/users";
    }

    // Memproses penghapusan user oleh admin
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("message", "Pengguna berhasil dihapus.");
        return "redirect:/admin/users";
    }
}