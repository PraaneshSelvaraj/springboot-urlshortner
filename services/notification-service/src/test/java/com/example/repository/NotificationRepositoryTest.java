package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.model.NotificationModel;
import com.example.model.NotificationStatusModel;
import com.example.model.NotificationTypeModel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
@DisplayName("NotificationRepository Tests")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@org.springframework.test.context.TestPropertySource(locations = "classpath:application-test.properties")
class NotificationRepositoryTest {

  private final TestEntityManager entityManager;
  private final NotificationRepository notificationRepository;

  private NotificationTypeModel testType;
  private NotificationStatusModel testStatus;

  @Autowired
  public NotificationRepositoryTest(
      TestEntityManager testEntityManager, NotificationRepository notificationRepository) {
    this.entityManager = testEntityManager;
    this.notificationRepository = notificationRepository;
  }

  @BeforeEach
  void setUp() {
    testType = new NotificationTypeModel();
    testType.setName("NEWURL");
    testType = entityManager.persistAndFlush(testType);

    testStatus = new NotificationStatusModel();
    testStatus.setName("SUCCESS");
    testStatus = entityManager.persistAndFlush(testStatus);
  }

  @Test
  @DisplayName("Should save and retrieve notification successfully")
  void shouldSaveAndRetrieveNotificationSuccessfully() {
    NotificationModel notification = new NotificationModel();
    notification.setMessage("Test notification");
    notification.setShortCode("abc123");
    notification.setType(testType);
    notification.setStatus(testStatus);

    NotificationModel savedNotification = notificationRepository.save(notification);

    assertThat(savedNotification.getId()).isNotNull();
    assertThat(savedNotification.getMessage()).isEqualTo("Test notification");
    assertThat(savedNotification.getShortCode()).isEqualTo("abc123");
    assertThat(savedNotification.getType().getName()).isEqualTo("NEWURL");
    assertThat(savedNotification.getStatus().getName()).isEqualTo("SUCCESS");
    assertThat(savedNotification.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should find notification by id")
  void shouldFindNotificationById() {
    NotificationModel notification = new NotificationModel();
    notification.setMessage("Test message");
    notification.setType(testType);
    notification.setStatus(testStatus);

    NotificationModel saved = entityManager.persistAndFlush(notification);

    Optional<NotificationModel> found = notificationRepository.findById(saved.getId());

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
    assertThat(found.get().getMessage()).isEqualTo("Test message");
  }

  @Test
  @DisplayName("Should return empty when notification not found by id")
  void shouldReturnEmptyWhenNotificationNotFoundById() {
    Optional<NotificationModel> found = notificationRepository.findById(999L);

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should save notification without short code")
  void shouldSaveNotificationWithoutShortCode() {
    NotificationModel notification = new NotificationModel();
    notification.setMessage("Notification without short code");
    notification.setType(testType);
    notification.setStatus(testStatus);

    NotificationModel saved = notificationRepository.save(notification);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getShortCode()).isNull();
    assertThat(saved.getMessage()).isEqualTo("Notification without short code");
  }

  @Test
  @DisplayName("Should find all notifications")
  void shouldFindAllNotifications() {
    NotificationModel notification1 = new NotificationModel();
    notification1.setMessage("First notification");
    notification1.setType(testType);
    notification1.setStatus(testStatus);

    NotificationModel notification2 = new NotificationModel();
    notification2.setMessage("Second notification");
    notification2.setType(testType);
    notification2.setStatus(testStatus);

    entityManager.persistAndFlush(notification1);
    entityManager.persistAndFlush(notification2);

    List<NotificationModel> notifications = notificationRepository.findAll();

    assertThat(notifications).hasSize(2);
  }

  @Test
  @DisplayName("Should find notifications with pagination")
  void shouldFindNotificationsWithPagination() {
    for (int i = 0; i < 15; i++) {
      NotificationModel notification = new NotificationModel();
      notification.setMessage("Notification " + i);
      notification.setType(testType);
      notification.setStatus(testStatus);
      entityManager.persist(notification);
    }
    entityManager.flush();

    PageRequest pageable = PageRequest.of(0, 10);
    Page<NotificationModel> page = notificationRepository.findAll(pageable);

    assertThat(page.getContent()).hasSize(10);
    assertThat(page.getTotalElements()).isEqualTo(15);
    assertThat(page.getTotalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should find notifications sorted by id descending")
  void shouldFindNotificationsSortedByIdDescending() {
    NotificationModel notification1 = new NotificationModel();
    notification1.setMessage("First");
    notification1.setType(testType);
    notification1.setStatus(testStatus);
    entityManager.persistAndFlush(notification1);

    NotificationModel notification2 = new NotificationModel();
    notification2.setMessage("Second");
    notification2.setType(testType);
    notification2.setStatus(testStatus);
    entityManager.persistAndFlush(notification2);

    PageRequest pageable = PageRequest.of(0, 10, Sort.by("id").descending());
    Page<NotificationModel> page = notificationRepository.findAll(pageable);

    assertThat(page.getContent().get(0).getMessage()).isEqualTo("Second");
    assertThat(page.getContent().get(1).getMessage()).isEqualTo("First");
  }

  @Test
  @DisplayName("Should delete notification")
  void shouldDeleteNotification() {
    NotificationModel notification = new NotificationModel();
    notification.setMessage("To be deleted");
    notification.setType(testType);
    notification.setStatus(testStatus);

    NotificationModel saved = entityManager.persistAndFlush(notification);
    Long id = saved.getId();

    notificationRepository.delete(saved);
    entityManager.flush();

    Optional<NotificationModel> found = notificationRepository.findById(id);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should count all notifications")
  void shouldCountAllNotifications() {
    NotificationModel notification1 = new NotificationModel();
    notification1.setMessage("First");
    notification1.setType(testType);
    notification1.setStatus(testStatus);

    NotificationModel notification2 = new NotificationModel();
    notification2.setMessage("Second");
    notification2.setType(testType);
    notification2.setStatus(testStatus);

    entityManager.persistAndFlush(notification1);
    entityManager.persistAndFlush(notification2);

    long count = notificationRepository.count();

    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("Should update notification message")
  void shouldUpdateNotificationMessage() {
    NotificationModel notification = new NotificationModel();
    notification.setMessage("Original message");
    notification.setType(testType);
    notification.setStatus(testStatus);

    NotificationModel saved = entityManager.persistAndFlush(notification);
    Long id = saved.getId();

    saved.setMessage("Updated message");
    notificationRepository.save(saved);
    entityManager.flush();
    entityManager.clear();

    NotificationModel updated = notificationRepository.findById(id).orElseThrow();
    assertThat(updated.getMessage()).isEqualTo("Updated message");
  }

  @Test
  @DisplayName("Should save notification with different types")
  void shouldSaveNotificationWithDifferentTypes() {
    NotificationTypeModel thresholdType = new NotificationTypeModel();
    thresholdType.setName("THRESHOLD");
    thresholdType = entityManager.persistAndFlush(thresholdType);

    NotificationModel notification = new NotificationModel();
    notification.setMessage("Threshold notification");
    notification.setType(thresholdType);
    notification.setStatus(testStatus);

    NotificationModel saved = notificationRepository.save(notification);

    assertThat(saved.getType().getName()).isEqualTo("THRESHOLD");
  }

  @Test
  @DisplayName("Should save notification with different statuses")
  void shouldSaveNotificationWithDifferentStatuses() {
    NotificationStatusModel pendingStatus = new NotificationStatusModel();
    pendingStatus.setName("PENDING");
    pendingStatus = entityManager.persistAndFlush(pendingStatus);

    NotificationModel notification = new NotificationModel();
    notification.setMessage("Pending notification");
    notification.setType(testType);
    notification.setStatus(pendingStatus);

    NotificationModel saved = notificationRepository.save(notification);

    assertThat(saved.getStatus().getName()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName("Should maintain created_at timestamp")
  void shouldMaintainCreatedAtTimestamp() {
    LocalDateTime before = LocalDateTime.now();

    NotificationModel notification = new NotificationModel();
    notification.setMessage("Test timestamp");
    notification.setType(testType);
    notification.setStatus(testStatus);

    NotificationModel saved = notificationRepository.save(notification);
    LocalDateTime after = LocalDateTime.now();

    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getCreatedAt()).isAfterOrEqualTo(before);
    assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(after);
  }
}
