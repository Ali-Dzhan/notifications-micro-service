package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.UpsertNotificationPreference;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationRepository notificationRepository;
    private final MailSender mailSender;

    @Autowired
    public NotificationService(NotificationPreferenceRepository preferenceRepository,
                               NotificationRepository notificationRepository,
                               MailSender mailSender) {
        this.preferenceRepository = preferenceRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public NotificationPreference upsertPreference(UpsertNotificationPreference dto) {
        Optional<NotificationPreference> existingOpt = preferenceRepository.findByUserId(dto.getUserId());

        if (existingOpt.isPresent()) {
            NotificationPreference preference = existingOpt.get();
            preference.setContactInfo(dto.getContactInfo());
            preference.setEnabled(dto.isNotificationEnabled());
            return preferenceRepository.save(preference);
        }
        NotificationPreference newPref = NotificationPreference.builder()
                .userId(dto.getUserId())
                .enabled(dto.isNotificationEnabled())
                .contactInfo(dto.getContactInfo())
                .build();

        return preferenceRepository.save(newPref);
    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Notification preference not found for user: " + userId
                ));
    }

    @Transactional
    public Notification sendNotification(NotificationRequest notificationRequest) {
        UUID userId = notificationRequest.getUserId();

        NotificationPreference userPreference = getPreferenceByUserId(userId);

        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications."
                    .formatted(userId));
        }

        if (userPreference.getContactInfo() != null && !userPreference.getContactInfo().isBlank()) {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userPreference.getContactInfo());
            message.setSubject(notificationRequest.getSubject());
            message.setText(notificationRequest.getBody());

            try {
                mailSender.send(message);
                log.info("Email sent to [{}]", userPreference.getContactInfo());
            } catch (Exception e) {
                log.warn("Failed to send email to [{}]: {}", userPreference.getContactInfo(), e.getMessage());
            }
        }

        Notification notification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .build();

        return notificationRepository.save(notification);
    }
    
    public List<Notification> getNotificationHistory(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Transactional
    public NotificationPreference changeNotificationPreference(UUID userId, boolean enabled) {
        NotificationPreference preference = getPreferenceByUserId(userId);
        preference.setEnabled(enabled);
        return preferenceRepository.save(preference);
    }
}
