package com.example.web_tiket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.web_tiket.model.Role;
import com.example.web_tiket.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                // Mengizinkan akses tanpa autentikasi ke halaman root, registrasi, login, daftar event, detail event, dan GAMBAR EVENT
                .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/events", "/events/{id}").permitAll()
                // Izinkan akses ke gambar event
                .requestMatchers("/events/image/{id}").permitAll()

                // Memastikan form order tiket memerlukan autentikasi
                .requestMatchers("/events/{id}/order").authenticated()

                // Izinkan USER dan ADMIN untuk melihat transaksi mereka sendiri, tiket/bukti, dan profil
                .requestMatchers("/my-transactions/**").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/admin/transactions/proof/{id}").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/my-transactions/ticket-image/{id}").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/my-transactions/ticket-details/{id}").hasAnyRole(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/profile", "/profile/update").hasAnyRole(Role.USER.name(), Role.ADMIN.name()) // <--- BARIS BARU INI

                // Membatasi akses ke URL yang diawali dengan /admin/** hanya untuk pengguna dengan peran ADMIN
                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())

                // Semua permintaan lainnya memerlukan autentikasi
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );
        return http.build();
    }
}