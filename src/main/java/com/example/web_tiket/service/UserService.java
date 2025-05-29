package com.example.web_tiket.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service; 

import com.example.web_tiket.model.Role;
import com.example.web_tiket.model.User;
import com.example.web_tiket.repository.UserRepository;

// Anotasi @Service menandakan bahwa kelas ini adalah komponen service Spring.
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Deklarasikan PasswordEncoder

    // Konstruktor untuk injeksi dependensi.
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) { // Suntikkan PasswordEncoder di sini
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; // Inisialisasi PasswordEncoder
    }

    /**
     * Mendaftarkan pengguna baru ke sistem.
     * Password akan DIENKRIPSI sebelum disimpan.
     * Secara default, pengguna baru akan memiliki peran USER.
     *
     * @param user Objek User yang akan didaftarkan.
     * @return User yang telah disimpan, atau null jika username/email sudah ada.
     */
    public User registerUser(User user) {
        // Cek apakah username atau email sudah ada
        if (userRepository.findByUsername(user.getUsername()).isPresent() ||
            userRepository.findByEmail(user.getEmail()).isPresent()) {
            System.out.println("Username or Email already exists.");
            return null; // Atau throw exception kustom
        }
        // --- PENTING: Password DIENKRIPSI SEBELUM DISIMPAN ---
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Gunakan encoder untuk mengenkripsi password
        
        // Set peran default sebagai USER
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    /**
     * Mencari pengguna berdasarkan username.
     * Digunakan untuk proses login dan validasi.
     *
     * @param username Username pengguna.
     * @return Optional yang berisi User jika ditemukan, atau kosong jika tidak.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Mengambil semua pengguna yang terdaftar.
     * Hanya boleh diakses oleh ADMIN.
     *
     * @return List dari semua objek User.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Mengambil pengguna berdasarkan ID.
     * Hanya boleh diakses oleh ADMIN.
     *
     * @param id ID pengguna.
     * @return Optional yang berisi User jika ditemukan, atau kosong jika tidak.
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Memperbarui data pengguna.
     * Hanya boleh diakses oleh ADMIN.
     * Password akan DIENKRIPSI jika diubah.
     *
     * @param id ID pengguna yang akan diperbarui.
     * @param updatedUser Objek User dengan data yang diperbarui.
     * @return User yang telah diperbarui, atau null jika pengguna tidak ditemukan.
     */
    public User updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(updatedUser.getUsername());
            user.setEmail(updatedUser.getEmail());
            // Hanya perbarui password jika password baru tidak kosong
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                // --- PENTING: Password DIENKRIPSI SEBELUM DISIMPAN ---
                user.setPassword(passwordEncoder.encode(updatedUser.getPassword())); // Gunakan encoder untuk mengenkripsi password
            }
            user.setRole(updatedUser.getRole()); // Admin bisa mengubah peran
            return userRepository.save(user);
        }).orElse(null);
    }

    /**
     * Menghapus pengguna berdasarkan ID.
     * Hanya boleh diakses oleh ADMIN.
     *
     * @param id ID pengguna yang akan dihapus.
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}