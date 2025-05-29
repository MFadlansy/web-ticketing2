package com.example.web_tiket.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map; // Tambahkan import ini
import java.util.HashMap; // Tambahkan import ini
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.web_tiket.model.Event;
import com.example.web_tiket.model.Transaction;
import com.example.web_tiket.model.Transaction.TransactionStatus;
import com.example.web_tiket.model.User;
import com.example.web_tiket.repository.EventRepository;
import com.example.web_tiket.repository.TransactionRepository;
import com.example.web_tiket.repository.UserRepository;

// Import ZXing classes
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              EventRepository eventRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Membuat transaksi pemesanan tiket baru.
     * Status awal adalah PENDING_UPLOAD dan batas waktu pembayaran diset 5 menit dari sekarang.
     *
     * @param userId ID pengguna yang memesan.
     * @param eventId ID event yang dipesan.
     * @param jumlahTiket Jumlah tiket yang dipesan.
     * @return Transaksi yang telah disimpan, atau null jika user/event tidak ditemukan.
     */
    public Transaction createTransaction(Long userId, Long eventId, Integer jumlahTiket) {
        Optional<User> userOptional = userRepository.findById(userId);
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (userOptional.isPresent() && eventOptional.isPresent()) {
            User user = userOptional.get();
            Event event = eventOptional.get();

            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setEvent(event);
            transaction.setJumlahTiket(jumlahTiket);
            transaction.setTotalHarga(event.getHargaTiket() * jumlahTiket);
            transaction.setStatus(TransactionStatus.PENDING_UPLOAD); // Status awal: menunggu upload bukti
            transaction.setTanggalTransaksi(LocalDateTime.now());
            transaction.setBatasWaktuPembayaran(LocalDateTime.now().plusMinutes(5)); // Batas waktu 5 menit

            return transactionRepository.save(transaction);
        }
        return null; // User atau Event tidak ditemukan
    }

    /**
     * Mengupload bukti pembayaran untuk transaksi tertentu.
     * Mengubah status menjadi PENDING_CONFIRMATION jika berhasil diupload.
     *
     * @param transactionId ID transaksi.
     * @param file File gambar bukti pembayaran.
     * @return Transaksi yang diperbarui, atau null jika transaksi tidak ditemukan.
     * @throws IOException jika gagal membaca file.
     */
    public Transaction uploadPaymentProof(Long transactionId, MultipartFile file) throws IOException {
        return transactionRepository.findById(transactionId).map(transaction -> {
            if (file != null && !file.isEmpty()) {
                try {
                    transaction.setBuktiPembayaran(file.getBytes());
                    transaction.setWaktuUploadBukti(LocalDateTime.now()); // Catat waktu upload
                    transaction.setStatus(TransactionStatus.PENDING_CONFIRMATION); // Bukti diupload, menunggu konfirmasi admin
                } catch (IOException e) {
                    throw new RuntimeException("Gagal membaca file bukti pembayaran", e);
                }
            }
            return transactionRepository.save(transaction);
        }).orElse(null);
    }

    /**
     * Mengambil semua transaksi (untuk Admin).
     * @return List dari semua objek Transaction.
     */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Mengambil transaksi berdasarkan ID.
     * @param id ID transaksi.
     * @return Optional yang berisi Transaction jika ditemukan, atau kosong jika tidak.
     */
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    /**
     * Mengambil semua transaksi milik user tertentu.
     * @param userId ID user.
     * @return List dari transaksi milik user.
     */
    public List<Transaction> getUserTransactions(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.map(transactionRepository::findByUser).orElse(List.of());
    }

    /**
     * Memperbarui status transaksi (untuk Admin).
     * Akan memicu pembuatan data tiket (QR Code) jika status baru adalah COMPLETED.
     *
     * @param id ID transaksi yang akan diperbarui.
     * @param newStatus Status baru transaksi.
     * @return Transaksi yang diperbarui, atau null jika transaksi tidak ditemukan.
     */
    public Transaction updateTransactionStatus(Long id, TransactionStatus newStatus) {
        return transactionRepository.findById(id).map(transaction -> {
            transaction.setStatus(newStatus);
            if (newStatus == TransactionStatus.COMPLETED) {
                try {
                    // Generate QR Code content
                    String qrCodeContent = String.format(
                        "Event: %s\nUser: %s\nEmail: %s\nTickets: %d\nTransaction ID: %d\n",
                        transaction.getEvent().getNamaEvent(),
                        transaction.getUser().getUsername(),
                        transaction.getUser().getEmail(),
                        transaction.getJumlahTiket(),
                        transaction.getId()
                    );
                    // Generate QR Code image
                    byte[] qrCodeImage = generateQrCodeImage(qrCodeContent, 200, 200); // Lebar dan tinggi gambar QR

                    transaction.setDataTiket(qrCodeImage); // Simpan gambar QR ke kolom data_tiket
                } catch (WriterException | IOException e) {
                    System.err.println("Gagal membuat QR Code untuk transaksi #" + transaction.getId() + ": " + e.getMessage());
                    // Handle error, maybe set status back or log it
                }
            }
            return transactionRepository.save(transaction);
        }).orElse(null);
    }

    /**
     * Memperbarui seluruh detail transaksi (untuk Admin).
     * @param id ID transaksi yang akan diperbarui.
     * @param updatedTransaction Objek Transaction dengan data yang diperbarui.
     * @return Transaksi yang telah diperbarui, atau null jika transaksi tidak ditemukan.
     */
    public Transaction updateTransaction(Long id, Transaction updatedTransaction) {
        return transactionRepository.findById(id).map(transaction -> {
            transaction.setJumlahTiket(updatedTransaction.getJumlahTiket());
            transaction.setTotalHarga(updatedTransaction.getTotalHarga());
            // Perbarui status jika berbeda
            if (transaction.getStatus() != updatedTransaction.getStatus()) {
                // Panggil logika updateStatus agar penanganan COMPLETED juga terpanggil
                return updateTransactionStatus(id, updatedTransaction.getStatus());
            }
            return transactionRepository.save(transaction);
        }).orElse(null);
    }

    /**
     * Menghapus transaksi.
     * @param id ID transaksi yang akan dihapus.
     */
    public void deleteTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    /**
     * Mencari transaksi dengan status PENDING_UPLOAD yang telah melewati batas waktu pembayaran.
     * Digunakan oleh scheduler untuk pembatalan otomatis.
     * @param dateTime Waktu batas atas untuk tanggal transaksi.
     * @return List transaksi yang kadaluarsa.
     */
    public List<Transaction> findExpiredPendingUploadTransactions(LocalDateTime dateTime) {
        return transactionRepository.findByStatusAndBatasWaktuPembayaranBefore(TransactionStatus.PENDING_UPLOAD, dateTime);
    }

    /**
     * Menghasilkan gambar QR Code dari string konten.
     * @param content Konten string untuk QR Code
     * @param width Lebar gambar QR Code
     * @param height Tinggi gambar QR Code
     * @return Byte array gambar QR Code (PNG)
     * @throws WriterException jika terjadi error saat menulis QR Code
     * @throws IOException jika terjadi error saat menulis gambar
     */
    private byte[] generateQrCodeImage(String content, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // Margin putih di sekitar QR code

        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}