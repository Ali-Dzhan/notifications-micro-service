package app.service;

import app.model.Notification;
import app.model.NotificationPreference;
import app.repository.NotificationPreferenceRepository;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import app.web.dto.UpsertNotificationPreference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceUTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private MailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void givenNotExistingNotificationPreference_whenChangeNotificationPreference_thenExpectException(){

        // Given
        UUID userId = UUID.randomUUID();
        boolean isNotificationEnabled = true;
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                notificationService.changeNotificationPreference(userId, isNotificationEnabled));
    }

    @Test
    void givenExistingNotificationPreference_whenChangeNotificationPreference_thenExpectEnabledToBeChanged(){

        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference preference = NotificationPreference.builder()
                .enabled(false)
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        // When
        notificationService.changeNotificationPreference(userId, true);

        // Then
        assertTrue(preference.isEnabled());
        verify(preferenceRepository, times(1)).save(preference);
    }

    @Test
    void upsertPreference_ShouldUpdateExistingPreference() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference existingPref = NotificationPreference.builder()
                .userId(userId)
                .enabled(false)
                .contactInfo("old@example.com")
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existingPref));

        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("new@example.com")
                .build();

        // When
        notificationService.upsertPreference(dto);

        // Then
        assertTrue(existingPref.isEnabled());
        assertEquals("new@example.com", existingPref.getContactInfo());
        verify(preferenceRepository).save(existingPref);
    }

    @Test
    void upsertPreference_ShouldCreateNewPreference_IfNotExisting() {
        // Given
        UUID userId = UUID.randomUUID();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // This line is crucial: ensures save(...) doesn't return null
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpsertNotificationPreference dto = UpsertNotificationPreference.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("someone@example.com")
                .build();

        // When
        NotificationPreference result = notificationService.upsertPreference(dto);

        // Then
        assertNotNull(result); // Now it won't be null
        assertEquals(userId, result.getUserId());
        assertTrue(result.isEnabled());
        assertEquals("someone@example.com", result.getContactInfo());
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    void getPreferenceByUserId_ShouldReturnPreference_IfFound() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .contactInfo("user@example.com")
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        // When
        NotificationPreference result = notificationService.getPreferenceByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("user@example.com", result.getContactInfo());
    }

    @Test
    void getPreferenceByUserId_ShouldThrowIllegalStateException_IfMissing() {
        // Given
        UUID userId = UUID.randomUUID();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                notificationService.getPreferenceByUserId(userId));
    }

    @Test
    void sendNotification_ShouldThrowIfPreferenceNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("Hello")
                .body("Test body")
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalStateException.class, () ->
                notificationService.sendNotification(request));
        verify(notificationRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotification_ShouldThrowIfPreferenceDisabled() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(false)
                .contactInfo("user@example.com")
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("Hello")
                .body("Test body")
                .build();

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                notificationService.sendNotification(request));
        verify(notificationRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendNotification_ShouldSendMailIfContactInfoPresent() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .contactInfo("user@example.com")
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("Hello")
                .body("Test body")
                .build();

        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .subject("Hello")
                .body("Test body")
                .createdOn(LocalDateTime.now())
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // When
        Notification result = notificationService.sendNotification(request);

        // Then
        assertNotNull(result);
        assertEquals("Hello", result.getSubject());
        assertEquals("Test body", result.getBody());
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendNotification_ShouldNotSendMailIfContactInfoBlank() {
        // Given
        UUID userId = UUID.randomUUID();
        NotificationPreference pref = NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .contactInfo("") // blank
                .build();
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

        NotificationRequest request = NotificationRequest.builder()
                .userId(userId)
                .subject("No email")
                .body("Test body")
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        // When
        Notification result = notificationService.sendNotification(request);

        // Then
        assertNotNull(result);
        assertEquals("No email", result.getSubject());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void getNotificationHistory_ShouldReturnListFromRepository() {
        // Given
        UUID userId = UUID.randomUUID();
        List<Notification> mockList = Arrays.asList(new Notification(), new Notification());
        when(notificationRepository.findByUserId(userId)).thenReturn(mockList);

        // When
        List<Notification> result = notificationService.getNotificationHistory(userId);

        // Then
        assertEquals(2, result.size());
        verify(notificationRepository).findByUserId(userId);
    }
}
