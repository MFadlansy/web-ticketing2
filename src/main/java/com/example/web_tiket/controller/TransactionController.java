package com.example.web_tiket.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.web_tiket.dto.TicketDetailsDto;
import com.example.web_tiket.model.Event;
import com.example.web_tiket.model.Role;
import com.example.web_tiket.model.Transaction;
import com.example.web_tiket.model.Transaction.TransactionStatus;
import com.example.web_tiket.model.User;
import com.example.web_tiket.repository.UserRepository;
import com.example.web_tiket.service.EventService;
import com.example.web_tiket.service.TransactionService; // Import DTO yang baru


@Controller
public class TransactionController {

    private final TransactionService transactionService;
    private final EventService eventService;
    private final UserRepository userRepository;

    @Autowired
    public TransactionController(TransactionService transactionService,
                                 EventService eventService,
                                 UserRepository userRepository) {
        this.transactionService = transactionService;
        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    // Helper method untuk mendapatkan user yang sedang login
    private Optional<User> getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    // --- Endpoints untuk User ---

    // Menampilkan form pemesanan tiket pada halaman detail event (URL tidak berubah)
    @GetMapping("/events/{id}/order")
    public String showOrderForm(@PathVariable("id") Long eventId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Event> eventOptional = eventService.getEventById(eventId);
        if (eventOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Event tidak ditemukan.");
            return "redirect:/events";
        }
        model.addAttribute("event", eventOptional.get());
        model.addAttribute("transaction", new Transaction());
        return "order-ticket";
    }

    // Memproses pemesanan tiket oleh user
    @PostMapping("/events/{id}/order")
    public String placeOrder(@PathVariable("id") Long eventId,
                             @ModelAttribute("transaction") Transaction transaction,
                             RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pengguna tidak ditemukan. Silakan login ulang.");
            return "redirect:/login";
        }

        Transaction newTransaction = transactionService.createTransaction(
                userOptional.get().getId(),
                eventId,
                transaction.getJumlahTiket()
        );

        if (newTransaction != null) {
            redirectAttributes.addFlashAttribute("message", "Pemesanan berhasil! Silakan upload bukti pembayaran untuk transaksi #" + newTransaction.getId() + ". Anda memiliki waktu 5 menit.");
            // Redirect ke halaman riwayat transaksi user, yang akan menampilkan link upload
            return "redirect:/my-transactions";
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal memesan tiket. Event atau User tidak ditemukan.");
            return "redirect:/events/" + eventId;
        }
    }

    // Menampilkan daftar transaksi milik user yang sedang login
    @GetMapping("/my-transactions")
    public String myTransactions(Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();

        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pengguna tidak ditemukan. Silakan login ulang.");
            return "redirect:/login";
        }

        List<Transaction> transactions = transactionService.getUserTransactions(userOptional.get().getId());
        model.addAttribute("transactions", transactions);
        model.addAttribute("transactionStatuses", TransactionStatus.values());
        model.addAttribute("localDateTime", LocalDateTime.now()); // Untuk perbandingan waktu di Thymeleaf
        return "my-transactions";
    }

    // Menampilkan form upload bukti pembayaran
    @GetMapping("/my-transactions/upload-proof/{id}")
    public String showUploadProofForm(@PathVariable("id") Long transactionId, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Autentikasi diperlukan.");
            return "redirect:/login";
        }

        Transaction transaction = transactionService.getTransactionById(transactionId).orElse(null);
        if (transaction == null || !transaction.getUser().getId().equals(userOptional.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Transaksi tidak ditemukan atau Anda tidak memiliki akses.");
            return "redirect:/my-transactions";
        }

        // Cek status dan batas waktu
        if (transaction.getStatus() != TransactionStatus.PENDING_UPLOAD) {
            redirectAttributes.addFlashAttribute("error", "Transaksi ini tidak dalam status menunggu upload bukti pembayaran.");
            return "redirect:/my-transactions";
        }
        if (transaction.getBatasWaktuPembayaran() != null && LocalDateTime.now().isAfter(transaction.getBatasWaktuPembayaran())) {
            redirectAttributes.addFlashAttribute("error", "Batas waktu pembayaran untuk transaksi ini sudah habis.");
            return "redirect:/my-transactions";
        }

        model.addAttribute("transaction", transaction);
        model.addAttribute("localDateTime", LocalDateTime.now()); // Untuk perbandingan waktu di Thymeleaf
        return "upload-proof";
    }

    // Memproses upload bukti pembayaran
    @PostMapping("/my-transactions/upload-proof/{id}")
    public String uploadPaymentProof(@PathVariable("id") Long transactionId,
                                     @RequestParam("file") MultipartFile file,
                                     RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Autentikasi diperlukan.");
            return "redirect:/login";
        }

        Transaction transaction = transactionService.getTransactionById(transactionId).orElse(null);
        if (transaction == null || !transaction.getUser().getId().equals(userOptional.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Transaksi tidak ditemukan atau Anda tidak memiliki akses.");
            return "redirect:/my-transactions";
        }

        // Cek status dan batas waktu
        if (transaction.getStatus() != TransactionStatus.PENDING_UPLOAD) {
            redirectAttributes.addFlashAttribute("error", "Transaksi ini tidak dalam status menunggu upload bukti pembayaran.");
            return "redirect:/my-transactions";
        }
        if (transaction.getBatasWaktuPembayaran() != null && LocalDateTime.now().isAfter(transaction.getBatasWaktuPembayaran())) {
            redirectAttributes.addFlashAttribute("error", "Batas waktu pembayaran untuk transaksi ini sudah habis.");
            return "redirect:/my-transactions";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Silakan pilih file bukti pembayaran.");
            return "redirect:/my-transactions/upload-proof/" + transactionId;
        }

        try {
            transactionService.uploadPaymentProof(transactionId, file);
            redirectAttributes.addFlashAttribute("message", "Bukti pembayaran berhasil diupload. Menunggu konfirmasi admin.");
            return "redirect:/my-transactions";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal mengupload bukti pembayaran: " + e.getMessage());
            return "redirect:/my-transactions/upload-proof/" + transactionId;
        }
    }

    /**
     * Endpoint untuk melayani gambar QR Code tiket.
     * Tidak lagi memaksa unduhan, tetapi hanya menampilkan gambar.
     * @param id ID transaksi
     * @param redirectAttributes untuk pesan error jika ada
     * @return ResponseEntity berisi byte[] gambar QR Code atau status error
     */
    @GetMapping("/my-transactions/ticket-image/{id}") // Nama endpoint baru untuk gambar tiket
    public ResponseEntity<byte[]> getTicketImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();
        if (userOptional.isEmpty()) {
            // Seharusnya sudah diatur oleh Spring Security, tapi untuk keamanan tambahan
            redirectAttributes.addFlashAttribute("error", "Autentikasi diperlukan untuk melihat tiket.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }

        User loggedInUser = userOptional.get();

        Transaction transaction = transactionService.getTransactionById(id).orElse(null);
        if (transaction == null) {
            redirectAttributes.addFlashAttribute("error", "Tiket tidak ditemukan.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
        }

        // Kontrol akses di sini: hanya pemilik atau ADMIN yang boleh melihat
        if (!transaction.getUser().getId().equals(loggedInUser.getId()) && loggedInUser.getRole() != Role.ADMIN) {
            redirectAttributes.addFlashAttribute("error", "Anda tidak memiliki akses untuk melihat tiket ini.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
        }

        // Pastikan tiket sudah COMPLETED dan data tiketnya ada
        if (transaction.getStatus() != TransactionStatus.COMPLETED || transaction.getDataTiket() == null) {
            redirectAttributes.addFlashAttribute("error", "Tiket belum siap atau tidak tersedia.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        byte[] ticketImage = transaction.getDataTiket();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG); // QR code akan disajikan sebagai PNG
        // Hapus: headers.setContentDispositionFormData("attachment", "ticket_" + transaction.getId() + ".png");
        headers.setContentLength(ticketImage.length);

        return new ResponseEntity<>(ticketImage, headers, HttpStatus.OK);
    }

    /**
     * Endpoint untuk menyediakan detail transaksi tiket dalam format JSON.
     * Digunakan oleh JavaScript untuk mengisi pop-up.
     * @param id ID transaksi
     * @param redirectAttributes untuk pesan error
     * @return ResponseEntity berisi TicketDetailsDto atau status error
     */
    @GetMapping("/my-transactions/ticket-details/{id}")
    public ResponseEntity<TicketDetailsDto> getTicketDetails(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }

        User loggedInUser = userOptional.get();

        Transaction transaction = transactionService.getTransactionById(id).orElse(null);
        if (transaction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
        }

        // Kontrol akses di sini: hanya pemilik atau ADMIN yang boleh melihat detail
        if (!transaction.getUser().getId().equals(loggedInUser.getId()) && loggedInUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
        }

        // Pastikan tiket sudah COMPLETED
        if (transaction.getStatus() != TransactionStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Tiket belum siap
        }

        TicketDetailsDto dto = new TicketDetailsDto(
            transaction.getId(),
            transaction.getEvent().getNamaEvent(),
            transaction.getUser().getUsername(),
            transaction.getUser().getEmail(),
            "/my-transactions/ticket-image/" + transaction.getId(), // URL untuk gambar QR code
            transaction.getTotalHarga(),
            transaction.getJumlahTiket()
        );

        return ResponseEntity.ok(dto);
    }


    // Mengelola (update/cancel) transaksi milik user (misal: hanya status PENDING)
    @PostMapping("/my-transactions/update-status/{id}")
    public String updateMyTransactionStatus(@PathVariable("id") Long id,
                                            @RequestParam("status") TransactionStatus newStatus,
                                            RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pengguna tidak ditemukan.");
            return "redirect:/login";
        }

        Transaction transaction = transactionService.getTransactionById(id).orElse(null);
        if (transaction == null || !transaction.getUser().getId().equals(userOptional.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Transaksi tidak ditemukan atau Anda tidak memiliki akses.");
            return "redirect:/my-transactions";
        }

        // Hanya izinkan user membatalkan transaksi yang masih PENDING_UPLOAD atau PENDING_CONFIRMATION
        if ((transaction.getStatus() == TransactionStatus.PENDING_UPLOAD || transaction.getStatus() == TransactionStatus.PENDING_CONFIRMATION) && newStatus == TransactionStatus.CANCELLED) {
            transactionService.updateTransactionStatus(transaction.getId(), newStatus); // Panggil service
            redirectAttributes.addFlashAttribute("message", "Transaksi berhasil dibatalkan.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Status transaksi tidak dapat diubah oleh Anda.");
        }
        return "redirect:/my-transactions";
    }

    // Menghapus transaksi milik user
    @GetMapping("/my-transactions/delete/{id}")
    public String deleteMyTransaction(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = getLoggedInUser();
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Pengguna tidak ditemukan.");
            return "redirect:/login";
        }

        Transaction transaction = transactionService.getTransactionById(id).orElse(null);
        if (transaction == null || !transaction.getUser().getId().equals(userOptional.get().getId())) {
            redirectAttributes.addFlashAttribute("error", "Transaksi tidak ditemukan atau Anda tidak memiliki akses.");
            return "redirect:/my-transactions";
        }

        // Batasi penghapusan hanya untuk status tertentu, misal CANCELLED atau PENDING_UPLOAD atau PENDING_CONFIRMATION
        if (transaction.getStatus() == TransactionStatus.PENDING_UPLOAD || transaction.getStatus() == TransactionStatus.PENDING_CONFIRMATION || transaction.getStatus() == TransactionStatus.CANCELLED) {
            transactionService.deleteTransaction(id);
            redirectAttributes.addFlashAttribute("message", "Transaksi berhasil dihapus.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Transaksi tidak dapat dihapus. Hanya transaksi pending atau dibatalkan yang bisa dihapus.");
        }

        return "redirect:/my-transactions";
    }


    // --- Endpoints untuk Admin ---

    // Menampilkan daftar semua transaksi untuk admin
    @GetMapping("/admin/transactions")
    public String adminListTransactions(Model model) {
        List<Transaction> transactions = transactionService.getAllTransactions();
        model.addAttribute("transactions", transactions);
        model.addAttribute("transactionStatuses", TransactionStatus.values()); // Untuk dropdown status
        return "admin/transactions";
    }

    // Menampilkan form edit transaksi untuk admin
    @GetMapping("/admin/transactions/edit/{id}")
    public String adminShowEditTransactionForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Transaction transaction = transactionService.getTransactionById(id).orElse(null);
        if (transaction == null) {
            redirectAttributes.addFlashAttribute("error", "Transaksi tidak ditemukan.");
            return "redirect:/admin/transactions";
        }
        model.addAttribute("transaction", transaction);
        model.addAttribute("transactionStatuses", TransactionStatus.values());
        return "admin/edit-transaction";
    }

    // Memproses pembaruan transaksi oleh admin
    @PostMapping("/admin/transactions/update/{id}")
    public String adminUpdateTransaction(@PathVariable("id") Long id,
                                    @ModelAttribute("transaction") Transaction transaction,
                                    RedirectAttributes redirectAttributes) {
        Transaction updated = transactionService.updateTransaction(id, transaction);
        if (updated != null) {
            redirectAttributes.addFlashAttribute("message", "Transaksi berhasil diperbarui.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Gagal memperbarui transaksi.");
        }
        return "redirect:/admin/transactions";
    }

    // Menghapus transaksi oleh admin
    @GetMapping("/admin/transactions/delete/{id}")
    public String adminDeleteTransaction(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            transactionService.deleteTransaction(id);
            redirectAttributes.addFlashAttribute("message", "Transaksi berhasil dihapus.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal menghapus transaksi: " + e.getMessage());
        }
        return "redirect:/admin/transactions";
    }

    // Endpoint untuk menampilkan bukti pembayaran oleh admin
    @GetMapping("/admin/transactions/proof/{id}")
    public ResponseEntity<byte[]> getPaymentProof(@PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(transaction -> {
                    byte[] imageData = transaction.getBuktiPembayaran();
                    if (imageData != null && imageData.length > 0) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.IMAGE_JPEG); // Asumsi gambar adalah JPEG
                        // Anda mungkin perlu logika untuk mendeteksi tipe gambar sebenarnya
                        return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
                    } else {
                        return ResponseEntity.notFound().<byte[]>build(); // Tambahkan <byte[]> di sini
                    }
                })
                .orElse(ResponseEntity.notFound().<byte[]>build());
    }
}