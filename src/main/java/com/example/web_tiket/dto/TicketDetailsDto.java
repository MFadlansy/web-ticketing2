package com.example.web_tiket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailsDto {
    private Long transactionId;
    private String eventName;
    private String username;
    private String userEmail;
    private String qrCodeImageUrl; // URL untuk menampilkan gambar QR code
    private Double totalHarga; // Tambahkan ini agar lengkap
    private Integer jumlahTiket; // Tambahkan ini agar lengkap
}