package com.example.web_tiket.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.web_tiket.model.Transaction;
import com.example.web_tiket.model.Transaction.TransactionStatus;
import com.example.web_tiket.model.User;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);

    // Metode baru untuk mencari transaksi yang statusnya PENDING_UPLOAD dan batas waktunya sudah lewat
    List<Transaction> findByStatusAndBatasWaktuPembayaranBefore(TransactionStatus status, LocalDateTime dateTime);
}