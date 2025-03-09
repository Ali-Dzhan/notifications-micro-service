package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.UpsertNotificationPreference;
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
    public NotificationService(NotificationPreferenceRepository preferenceRepository, NotificationRepository notificationRepository, MailSender mailSender) {
        this.preferenceRepository = preferenceRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    public NotificationPreference upsertPreference(UpsertNotificationPreference dto) {
        Optional<NotificationPreference> userPreferenceOpt = preferenceRepository.findByUserId(dto.getUserId());

        if (userPreferenceOpt.isPresent()) {
            NotificationPreference preference = userPreferenceOpt.get();
            preference.setContactInfo(dto.getContactInfo());
            preference.setEnabled(dto.isNotificationEnabled());
            return preferenceRepository.save(preference);
        }

        NotificationPreference preference = NotificationPreference.builder()
                .userId(dto.getUserId())
                .enabled(dto.isNotificationEnabled())
                .contactInfo(dto.getContactInfo())
                .build();

        return preferenceRepository.save(preference);
    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            NotificationPreference defaultPreference = NotificationPreference.builder()
                    .userId(userId)
                    .enabled(true)
                    .contactInfo("")
                    .build();
            return preferenceRepository.save(defaultPreference);
        });
    }

    public Notification sendNotification(NotificationRequest notificationRequest) {

        UUID userId = notificationRequest.getUserId();
        NotificationPreference userPreference = getPreferenceByUserId(userId);

        if (!userPreference.isEnabled()) {
            throw new IllegalArgumentException("User with id %s does not allow to receive notifications.".formatted(userId));
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userPreference.getContactInfo());
        message.setSubject(notificationRequest.getSubject());
        message.setText(notificationRequest.getBody());
        message.setFrom("alidzhansadak04@gmail.com");

        Notification notification = Notification.builder()
                .subject(notificationRequest.getSubject())
                .body(notificationRequest.getBody())
                .createdOn(LocalDateTime.now())
                .userId(userId)
                .build();

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("There was an issue sending an email to %s due to %s.".formatted(userPreference.getContactInfo(), e.getMessage()));
        }

        return notificationRepository.save(notification);
    }

    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndSeenFalse(userId);
    }

    public List<Notification> getNotificationHistory(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndSeenFalse(userId);
        notifications.forEach(n -> n.setSeen(true));
        notificationRepository.saveAll(notifications);
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public NotificationPreference changeNotificationPreference(UUID userId, boolean enabled) {

        NotificationPreference notificationPreference = getPreferenceByUserId(userId);
        notificationPreference.setEnabled(enabled);
        return preferenceRepository.save(notificationPreference);
    }
}
