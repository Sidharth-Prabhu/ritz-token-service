package com.rit.token.ritz.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);
    long countByRecipientIdAndReadFalse(String recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId")
    void markAllAsRead(@Param("recipientId") String recipientId);
}
