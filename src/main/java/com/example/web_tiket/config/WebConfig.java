package com.example.web_tiket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Anotasi @Configuration menandakan bahwa kelas ini berisi definisi bean Spring.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Metode ini digunakan untuk menambahkan view controllers yang memungkinkan
    // pemetaan langsung URL ke nama view atau redirect tanpa perlu controller eksplisit.
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Mendaftarkan view controller untuk URL root ("/")
        // Ketika pengguna mengakses "/", mereka akan di-redirect ke "/events".
        registry.addRedirectViewController("/", "/events");
    }
}
