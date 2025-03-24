package app;

import app.model.Notification;
import app.model.NotificationPreference;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class TestBuilder {

    public static NotificationPreference aRandomNotificationPreference() {

        return NotificationPreference.builder()
                .userId(UUID.randomUUID())
                .enabled(true)
                .contactInfo("text")
                .build();
    }

    public static Notification aRandomNotification() {
        return Notification.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .subject("text")
                .body("test body")
                .createdOn(LocalDateTime.now())
                .build();
    }
}
