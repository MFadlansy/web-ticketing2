package com.example.web_tiket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebTiketApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebTiketApplication.class, args);
	}

}
