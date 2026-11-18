package com.rit.token.ritz.ledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByTransactionIdOrderByCreatedAtAsc(String transactionId);

    Page<LedgerEntry> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);

    Page<LedgerEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amountMinorUnits), 0) FROM LedgerEntry e WHERE e.entryType = 'DEBIT'")
    Long getTotalDebitVolume();

    @Query("SELECT COALESCE(SUM(e.amountMinorUnits), 0) FROM LedgerEntry e WHERE e.entryType = 'CREDIT'")
    Long getTotalCreditVolume();

    @Query("SELECT COUNT(e) FROM LedgerEntry e WHERE e.transactionId = :transactionId")
    long countByTransactionId(@Param("transactionId") String transactionId);
}
