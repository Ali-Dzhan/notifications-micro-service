package app.web;

import app.service.NotificationService;
import app.web.dto.UpsertNotificationPreference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static app.TestBuilder.aRandomNotification;
import static app.TestBuilder.aRandomNotificationPreference;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
public class NotificationControllerApiTest {

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRequestNotificationPreference_happyPath() throws Exception {
        // 1. Build Request
        when(notificationService.getPreferenceByUserId(any())).thenReturn(aRandomNotificationPreference());
        MockHttpServletRequestBuilder request =
                get("/api/v1/notifications/preferences")
                        .param("userId", UUID.randomUUID().toString());

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());
    }

    @Test
    void postWithBodyToCreatePreference_returns201AndCorrectDtoStructure() throws Exception {
        // 1. Build Request
        UpsertNotificationPreference requestDto = UpsertNotificationPreference.builder()
                .userId(UUID.randomUUID())
                .contactInfo("text")
                .notificationEnabled(true)
                .build();

        when(notificationService.upsertPreference(any())).thenReturn(aRandomNotificationPreference());
        MockHttpServletRequestBuilder request = post("/api/v1/notifications/preferences")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsBytes(requestDto));

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());
    }

    @Test
    void putChangeNotificationPreference_happyPath() throws Exception {
        // 1. Build Request
        when(notificationService.changeNotificationPreference(any(), anyBoolean()))
                .thenReturn(aRandomNotificationPreference());

        MockHttpServletRequestBuilder request = put("/api/v1/notifications/preferences")
                .param("userId", UUID.randomUUID().toString())
                .param("enabled", "true");

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("userId").isNotEmpty())
                .andExpect(jsonPath("enabled").isNotEmpty())
                .andExpect(jsonPath("contactInfo").isNotEmpty());
    }

    @Test
    void getNotificationHistory_happyPath() throws Exception {
        // 1. Build Request
        when(notificationService.getNotificationHistory(any()))
                .thenReturn(List.of(aRandomNotification(), aRandomNotification()));

        MockHttpServletRequestBuilder request = get("/api/v1/notifications")
                .param("userId", UUID.randomUUID().toString());

        // 2. Send Request
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].userId").isNotEmpty())
                .andExpect(jsonPath("$[0].subject").isNotEmpty())
                .andExpect(jsonPath("$[0].body").isNotEmpty())
                .andExpect(jsonPath("$[0].createdOn").isNotEmpty());
    }
}
