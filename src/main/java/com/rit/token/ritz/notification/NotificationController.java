package com.rit.token.ritz.notification;

import com.rit.token.ritz.auth.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications for recharge, purchase, refund and alerts")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private String getRecipientId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof User user) {
            if (user.getStudentId() != null) return user.getStudentId();
            if (user.getMerchantId() != null) return user.getMerchantId();
            return user.getUsername();
        }
        return auth != null ? auth.getName() : "ANONYMOUS";
    }

    @GetMapping
    @Operation(summary = "Get user notifications and unread count")
    public ResponseEntity<Map<String, Object>> getNotifications(Authentication authentication) {
        String recipientId = getRecipientId(authentication);
        List<Notification> notifications = notificationService.getNotificationsForUser(recipientId);
        long unread = notificationService.getUnreadCount(recipientId);

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("unread_count", unread);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a single notification as read")
    public ResponseEntity<Map<String, Boolean>> markAsRead(@PathVariable Long id, Authentication authentication) {
        String recipientId = getRecipientId(authentication);
        boolean success = notificationService.markAsRead(id, recipientId);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        String recipientId = getRecipientId(authentication);
        notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
