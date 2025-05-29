// mfadlansy/web-ticketing/web-ticketing-3e64247b7580408a08f558843e96b55c1b4818e9/src/main/java/com/example/web_tiket/config/SecurityConfig.java
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
                // >>> Tambahkan baris ini untuk mengizinkan akses ke gambar event <<<
                .requestMatchers("/events/image/{id}").permitAll() // Izinkan akses ke gambar event

                // Memastikan form order tiket memerlukan autentikasi
                .requestMatchers("/events/{id}/order").authenticated()
                
                // Membatasi akses ke URL yang diawali dengan /admin/** hanya untuk pengguna dengan peran ADMIN
                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())
                // Hanya user dengan role USER yang bisa melihat transaksi mereka sendiri
                .requestMatchers("/my-transactions/**").hasRole(Role.USER.name())
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