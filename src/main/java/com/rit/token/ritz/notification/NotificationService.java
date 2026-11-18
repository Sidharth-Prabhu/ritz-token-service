package com.rit.token.ritz.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification sendNotification(String recipientId, String recipientType, String title, String message, NotificationType type) {
        try {
            Notification notification = new Notification(recipientId, recipientType, title, message, type);
            Notification saved = repository.save(notification);
            log.info("Notification sent to {} ({}): {}", recipientId, recipientType, title);
            return saved;
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            return null;
        }
    }

    public List<Notification> getNotificationsForUser(String recipientId) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    public Page<Notification> getNotificationsForUser(String recipientId, Pageable pageable) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
    }

    public long getUnreadCount(String recipientId) {
        return repository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Transactional
    public boolean markAsRead(Long id, String recipientId) {
        return repository.findById(id).map(notif -> {
            if (notif.getRecipientId().equals(recipientId)) {
                notif.setRead(true);
                repository.save(notif);
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Transactional
    public void markAllAsRead(String recipientId) {
        repository.markAllAsRead(recipientId);
    }
}
