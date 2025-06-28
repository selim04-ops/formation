package com.esprit.formation.repository;

import com.esprit.formation.entities.Transaction;
import com.esprit.formation.entities.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.transactionStatus = :status")
    List<Transaction> findByStatus(TransactionStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Transaction t SET t.transactionStatus = :newStatus WHERE t.id = :id")
    void updateTransactionStatus(@Param("id") Long id, @Param("newStatus") TransactionStatus newStatus);

    @Query("SELECT t FROM Transaction t WHERE t.transactionStatus = :status")
    List<Transaction> findByTransactionStatus(TransactionStatus status);

    @Query("SELECT tf.formationId as formationId, f.titre as title, COUNT(t) as count " +
            "FROM Transaction t JOIN t.formations tf " +
            "JOIN Formation f ON tf.formationId = f.id " +
            "WHERE t.transactionStatus = 'CONFIRMED' " +
            "GROUP BY tf.formationId, f.titre")
    List<FormationProjection> countParticipantsByFormation();

    @Query("SELECT CAST(t.confirmedAt AS date) as date, COUNT(t) as count " +
            "FROM Transaction t " +
            "WHERE t.transactionStatus = 'CONFIRMED' AND t.confirmedAt IS NOT NULL " +
            "GROUP BY CAST(t.confirmedAt AS date) " +
            "ORDER BY CAST(t.confirmedAt AS date)")
    List<DateProjection> countTransactionsByDate();

    interface FormationProjection {
        Long getFormationId();
        String getTitle();
        Long getCount();
    }

    interface DateProjection {
        LocalDate getDate();
        Long getCount();
    }

}