package com.example.web_tiket.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import ini untuk memastikan operasi DB atomik

import com.example.web_tiket.model.Transaction;
import com.example.web_tiket.model.Transaction.TransactionStatus;
import com.example.web_tiket.service.TransactionService;

@Component
public class PaymentCancellationScheduler {

    private final TransactionService transactionService;

    @Autowired
    public PaymentCancellationScheduler(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Metode ini akan dijalankan setiap 30 detik (30000 milidetik)
    // Untuk tujuan testing, bisa diatur lebih cepat. Untuk produksi, sesuaikan.
    @Scheduled(fixedRate = 30000)
    @Transactional // Penting: Pastikan operasi database dalam satu transaksi untuk konsistensi
    public void cancelExpiredPendingUploadTransactions() {
        System.out.println("Scheduler: Memeriksa transaksi PENDING_UPLOAD yang kadaluarsa pada " + LocalDateTime.now());

        // Batas waktu adalah 5 menit dari waktu transaksi dibuat
        LocalDateTime threshold = LocalDateTime.now();

        List<Transaction> expiredTransactions = transactionService.findExpiredPendingUploadTransactions(threshold);

        if (!expiredTransactions.isEmpty()) {
            for (Transaction transaction : expiredTransactions) {
                // Pastikan transaksi belum di-upload bukti bayar dan statusnya masih PENDING_UPLOAD
                if (transaction.getStatus() == TransactionStatus.PENDING_UPLOAD &&
                    (transaction.getBuktiPembayaran() == null || transaction.getBuktiPembayaran().length == 0)) {
                    
                    transactionService.updateTransactionStatus(transaction.getId(), TransactionStatus.CANCELLED);
                    System.out.println("Scheduler: Transaksi #" + transaction.getId() + " dibatalkan otomatis karena batas waktu pembayaran habis.");
                }
            }
            System.out.println("Scheduler: Selesai memeriksa transaksi. Total dibatalkan: " + expiredTransactions.size());
        } else {
            System.out.println("Scheduler: Tidak ada transaksi PENDING_UPLOAD yang kadaluarsa ditemukan.");
        }
    }
}