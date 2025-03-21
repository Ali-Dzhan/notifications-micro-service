package app.web.mapper;

import app.model.Notification;
import app.model.NotificationPreference;
import app.web.dto.NotificationPreferenceResponse;
import app.web.dto.NotificationResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {

    public static NotificationPreferenceResponse fromNotificationPreference(NotificationPreference entity) {

        return NotificationPreferenceResponse.builder()
                .userId(entity.getUserId())
                .contactInfo(entity.getContactInfo())
                .enabled(entity.isEnabled())
                .build();
    }

    public static NotificationResponse fromNotification(Notification entity) {

        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .subject(entity.getSubject())
                .body(entity.getBody())
                .createdOn(entity.getCreatedOn())
                .build();
    }
}
