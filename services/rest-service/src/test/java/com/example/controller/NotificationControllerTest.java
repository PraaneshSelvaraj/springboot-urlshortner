package com.example.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.dto.NotificationDto;
import com.example.dto.PagedNotificationsDto;
import com.example.service.NotificationService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationController.class, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, classes = com.example.filter.RateLimitFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private NotificationService notificationService;

  @MockBean private com.example.util.JwtUtil jwtUtil;

  @MockBean private com.example.service.TokenBlacklistService tokenBlacklistService;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should get paginated notifications successfully")
  void shouldGetPaginatedNotificationsSuccessfully() throws Exception {
    NotificationDto notification1 = new NotificationDto();
    notification1.setId(1L);
    notification1.setMessage("URL created");
    notification1.setShortCode("abc123");
    notification1.setNotificationType("NEWURL");
    notification1.setNotificationStatus("PENDING");

    NotificationDto notification2 = new NotificationDto();
    notification2.setId(2L);
    notification2.setMessage("Threshold reached");
    notification2.setShortCode("xyz789");
    notification2.setNotificationType("THRESHOLD");
    notification2.setNotificationStatus("SUCCESS");

    List<NotificationDto> notifications = Arrays.asList(notification1, notification2);

    PagedNotificationsDto pagedNotifications = new PagedNotificationsDto();
    pagedNotifications.setNotifications(notifications);
    pagedNotifications.setPageNo(0);
    pagedNotifications.setPageSize(10);
    pagedNotifications.setTotalPages(1);
    pagedNotifications.setTotalElements(2);

    when(notificationService.getNotifications(0, 10, null, null)).thenReturn(pagedNotifications);

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifications").isArray())
        .andExpect(jsonPath("$.notifications[0].id").value(1))
        .andExpect(jsonPath("$.notifications[0].message").value("URL created"))
        .andExpect(jsonPath("$.notifications[0].shortCode").value("abc123"))
        .andExpect(jsonPath("$.notifications[1].id").value(2))
        .andExpect(jsonPath("$.notifications[1].message").value("Threshold reached"))
        .andExpect(jsonPath("$.pageNo").value(0))
        .andExpect(jsonPath("$.pageSize").value(10))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.totalElements").value(2));

    verify(notificationService).getNotifications(0, 10, null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should get notifications with default pagination parameters")
  void shouldGetNotificationsWithDefaultPaginationParameters() throws Exception {
    PagedNotificationsDto emptyNotifications = new PagedNotificationsDto();
    emptyNotifications.setNotifications(Arrays.asList());
    emptyNotifications.setPageNo(0);
    emptyNotifications.setPageSize(10);
    emptyNotifications.setTotalPages(0);
    emptyNotifications.setTotalElements(0);

    when(notificationService.getNotifications(0, 10, null, null)).thenReturn(emptyNotifications);

    mockMvc
        .perform(get("/api/notifications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifications").isArray())
        .andExpect(jsonPath("$.notifications").isEmpty())
        .andExpect(jsonPath("$.pageNo").value(0))
        .andExpect(jsonPath("$.pageSize").value(10));

    verify(notificationService).getNotifications(0, 10, null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should throw exception when page number is negative")
  void shouldThrowExceptionWhenPageNumberIsNegative() throws Exception {
    when(notificationService.getNotifications(eq(-1), eq(10), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page number cannot be negative"));

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "-1").param("pageSize", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Page number cannot be negative"));

    verify(notificationService).getNotifications(eq(-1), eq(10), isNull(), isNull());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should throw exception when page size is zero")
  void shouldThrowExceptionWhenPageSizeIsZero() throws Exception {
    when(notificationService.getNotifications(eq(0), eq(0), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page size must be greater than zero."));

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "0").param("pageSize", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Page size must be greater than zero."));

    verify(notificationService).getNotifications(eq(0), eq(0), isNull(), isNull());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should throw exception when page size is negative")
  void shouldThrowExceptionWhenPageSizeIsNegative() throws Exception {
    when(notificationService.getNotifications(eq(0), eq(-5), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page size must be greater than zero."));

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "0").param("pageSize", "-5"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Page size must be greater than zero."));

    verify(notificationService).getNotifications(eq(0), eq(-5), isNull(), isNull());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should handle empty notifications list")
  void shouldHandleEmptyNotificationsList() throws Exception {
    PagedNotificationsDto emptyNotifications = new PagedNotificationsDto();
    emptyNotifications.setNotifications(Arrays.asList());
    emptyNotifications.setPageNo(0);
    emptyNotifications.setPageSize(10);
    emptyNotifications.setTotalPages(0);
    emptyNotifications.setTotalElements(0);

    when(notificationService.getNotifications(0, 10, null, null)).thenReturn(emptyNotifications);

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifications").isArray())
        .andExpect(jsonPath("$.notifications").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));

    verify(notificationService).getNotifications(0, 10, null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should handle large page size")
  void shouldHandleLargePageSize() throws Exception {
    PagedNotificationsDto notifications = new PagedNotificationsDto();
    notifications.setNotifications(Arrays.asList());
    notifications.setPageNo(0);
    notifications.setPageSize(1000);
    notifications.setTotalPages(0);
    notifications.setTotalElements(0);

    when(notificationService.getNotifications(0, 1000, null, null)).thenReturn(notifications);

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "0").param("pageSize", "1000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageSize").value(1000));

    verify(notificationService).getNotifications(0, 1000, null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should handle pagination with specific page number")
  void shouldHandlePaginationWithSpecificPageNumber() throws Exception {
    PagedNotificationsDto notifications = new PagedNotificationsDto();
    notifications.setNotifications(Arrays.asList());
    notifications.setPageNo(5);
    notifications.setPageSize(20);
    notifications.setTotalPages(10);
    notifications.setTotalElements(200);

    when(notificationService.getNotifications(5, 20, null, null)).thenReturn(notifications);

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "5").param("pageSize", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pageNo").value(5))
        .andExpect(jsonPath("$.pageSize").value(20))
        .andExpect(jsonPath("$.totalPages").value(10));

    verify(notificationService).getNotifications(5, 20, null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should return all notification types")
  void shouldReturnAllNotificationTypes() throws Exception {
    NotificationDto newUrlNotification = new NotificationDto();
    newUrlNotification.setId(1L);
    newUrlNotification.setNotificationType("NEWURL");
    newUrlNotification.setMessage("New URL created");

    NotificationDto thresholdNotification = new NotificationDto();
    thresholdNotification.setId(2L);
    thresholdNotification.setNotificationType("THRESHOLD");
    thresholdNotification.setMessage("Threshold reached");

    NotificationDto newUserNotification = new NotificationDto();
    newUserNotification.setId(3L);
    newUserNotification.setNotificationType("NEWUSER");
    newUserNotification.setMessage("New user registered");

    List<NotificationDto> notifications =
        Arrays.asList(newUrlNotification, thresholdNotification, newUserNotification);

    PagedNotificationsDto pagedNotifications = new PagedNotificationsDto();
    pagedNotifications.setNotifications(notifications);
    pagedNotifications.setPageNo(0);
    pagedNotifications.setPageSize(10);
    pagedNotifications.setTotalPages(1);
    pagedNotifications.setTotalElements(3);

    when(notificationService.getNotifications(0, 10, null, null)).thenReturn(pagedNotifications);

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifications[0].notificationType").value("NEWURL"))
        .andExpect(jsonPath("$.notifications[1].notificationType").value("THRESHOLD"))
        .andExpect(jsonPath("$.notifications[2].notificationType").value("NEWUSER"));

    verify(notificationService).getNotifications(0, 10, null, null);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Should return all notification statuses")
  void shouldReturnAllNotificationStatuses() throws Exception {
    NotificationDto pendingNotification = new NotificationDto();
    pendingNotification.setId(1L);
    pendingNotification.setNotificationStatus("PENDING");

    NotificationDto successNotification = new NotificationDto();
    successNotification.setId(2L);
    successNotification.setNotificationStatus("SUCCESS");

    NotificationDto failureNotification = new NotificationDto();
    failureNotification.setId(3L);
    failureNotification.setNotificationStatus("FAILURE");

    List<NotificationDto> notifications =
        Arrays.asList(pendingNotification, successNotification, failureNotification);

    PagedNotificationsDto pagedNotifications = new PagedNotificationsDto();
    pagedNotifications.setNotifications(notifications);
    pagedNotifications.setPageNo(0);
    pagedNotifications.setPageSize(10);
    pagedNotifications.setTotalPages(1);
    pagedNotifications.setTotalElements(3);

    when(notificationService.getNotifications(0, 10, null, null)).thenReturn(pagedNotifications);

    mockMvc
        .perform(get("/api/notifications").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notifications[0].notificationStatus").value("PENDING"))
        .andExpect(jsonPath("$.notifications[1].notificationStatus").value("SUCCESS"))
        .andExpect(jsonPath("$.notifications[2].notificationStatus").value("FAILURE"));

    verify(notificationService).getNotifications(0, 10, null, null);
  }
}
