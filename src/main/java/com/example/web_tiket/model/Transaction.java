package com.example.web_tiket.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private Integer jumlahTiket;

    @Column(nullable = false)
    private Double totalHarga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private LocalDateTime tanggalTransaksi;

    // --- New Fields for Payment Proof & Deadline ---
    @Lob // Stores as BLOB/LONGBLOB in database
    @Column(name = "bukti_pembayaran", columnDefinition="LONGBLOB", nullable = true) // Allow null initially
    private byte[] buktiPembayaran;

    @Column(name = "waktu_upload_bukti", nullable = true) // Timestamp when proof was uploaded
    private LocalDateTime waktuUploadBukti;

    @Column(name = "batas_waktu_pembayaran", nullable = true) // Batas waktu untuk upload bukti pembayaran
    private LocalDateTime batasWaktuPembayaran;

    // --- New Field for Ticket Data (QR Code Image) ---
    @Lob
    @Column(name = "data_tiket", columnDefinition="LONGBLOB", nullable = true) // Akan menyimpan gambar QR code sebagai tiket
    private byte[] dataTiket;


    // Enum untuk status transaksi (diperbarui)
    public enum TransactionStatus {
        PENDING_UPLOAD,     // Menunggu bukti pembayaran diupload (setelah user klik beli)
        PENDING_CONFIRMATION, // Bukti pembayaran sudah diupload, menunggu konfirmasi admin
        CONFIRMED,          // Pembayaran dikonfirmasi admin (admin ubah status jadi confirmed)
        COMPLETED,          // Tiket sudah dibuat dan siap diunduh (admin ubah status jadi completed)
        CANCELLED           // Dibatalkan (karena waktu habis atau oleh user/admin)
    }
}