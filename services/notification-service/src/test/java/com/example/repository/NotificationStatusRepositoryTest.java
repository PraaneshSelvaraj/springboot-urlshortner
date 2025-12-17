package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.model.NotificationStatusModel;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@DisplayName("NotificationStatusRepository Tests")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class NotificationStatusRepositoryTest {

  private final TestEntityManager entityManager;
  private final NotificationStatusRepository notificationStatusRepository;

  @Autowired
  public NotificationStatusRepositoryTest(
      TestEntityManager entityManager, NotificationStatusRepository notificationStatusRepository) {
    this.entityManager = entityManager;
    this.notificationStatusRepository = notificationStatusRepository;
  }

  @Test
  @DisplayName("Should save and retrieve notification status successfully")
  void shouldSaveAndRetrieveNotificationStatusSuccessfully() {
    NotificationStatusModel status = new NotificationStatusModel();
    status.setName("SUCCESS");

    NotificationStatusModel saved = notificationStatusRepository.save(status);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("SUCCESS");
  }

  @Test
  @DisplayName("Should find notification status by name")
  void shouldFindNotificationStatusByName() {
    NotificationStatusModel status = new NotificationStatusModel();
    status.setName("PENDING");
    entityManager.persistAndFlush(status);

    Optional<NotificationStatusModel> found = notificationStatusRepository.findByName("PENDING");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName("Should return empty when notification status not found by name")
  void shouldReturnEmptyWhenNotificationStatusNotFoundByName() {
    Optional<NotificationStatusModel> found =
        notificationStatusRepository.findByName("NONEXISTENT");

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find notification status by id")
  void shouldFindNotificationStatusById() {
    NotificationStatusModel status = new NotificationStatusModel();
    status.setName("FAILURE");

    NotificationStatusModel saved = entityManager.persistAndFlush(status);

    Optional<NotificationStatusModel> found =
        notificationStatusRepository.findById(saved.getId().longValue());

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
    assertThat(found.get().getName()).isEqualTo("FAILURE");
  }

  @Test
  @DisplayName("Should find all notification statuses")
  void shouldFindAllNotificationStatuses() {
    NotificationStatusModel status1 = new NotificationStatusModel();
    status1.setName("SUCCESS");

    NotificationStatusModel status2 = new NotificationStatusModel();
    status2.setName("PENDING");

    entityManager.persistAndFlush(status1);
    entityManager.persistAndFlush(status2);

    List<NotificationStatusModel> statuses = notificationStatusRepository.findAll();

    assertThat(statuses).hasSize(2);
    assertThat(statuses)
        .extracting(NotificationStatusModel::getName)
        .contains("SUCCESS", "PENDING");
  }

  @Test
  @DisplayName("Should delete notification status")
  void shouldDeleteNotificationStatus() {
    NotificationStatusModel status = new NotificationStatusModel();
    status.setName("TO_DELETE");

    NotificationStatusModel saved = entityManager.persistAndFlush(status);
    Integer id = saved.getId();

    notificationStatusRepository.delete(saved);
    entityManager.flush();

    Optional<NotificationStatusModel> found = notificationStatusRepository.findById(id.longValue());
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should count notification statuses")
  void shouldCountNotificationStatuses() {
    NotificationStatusModel status1 = new NotificationStatusModel();
    status1.setName("STATUS1");

    NotificationStatusModel status2 = new NotificationStatusModel();
    status2.setName("STATUS2");

    entityManager.persistAndFlush(status1);
    entityManager.persistAndFlush(status2);

    long count = notificationStatusRepository.count();

    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("Should update notification status name")
  void shouldUpdateNotificationStatusName() {
    NotificationStatusModel status = new NotificationStatusModel();
    status.setName("ORIGINAL");

    NotificationStatusModel saved = entityManager.persistAndFlush(status);
    Integer id = saved.getId();

    saved.setName("UPDATED");
    notificationStatusRepository.save(saved);
    entityManager.flush();
    entityManager.clear();

    NotificationStatusModel updated =
        notificationStatusRepository.findById(id.longValue()).orElseThrow();
    assertThat(updated.getName()).isEqualTo("UPDATED");
  }

  @Test
  @DisplayName("Should handle case-sensitive name search")
  void shouldHandleCaseSensitiveNameSearch() {
    NotificationStatusModel status = new NotificationStatusModel();
    status.setName("SUCCESS");
    entityManager.persistAndFlush(status);

    Optional<NotificationStatusModel> foundUpper =
        notificationStatusRepository.findByName("SUCCESS");
    Optional<NotificationStatusModel> foundLower =
        notificationStatusRepository.findByName("success");

    assertThat(foundUpper).isPresent();
    assertThat(foundLower).isEmpty();
  }

  @Test
  @DisplayName("Should return empty list when no statuses exist")
  void shouldReturnEmptyListWhenNoStatusesExist() {
    List<NotificationStatusModel> statuses = notificationStatusRepository.findAll();

    assertThat(statuses).isEmpty();
  }

  @Test
  @DisplayName("Should save multiple notification statuses with different names")
  void shouldSaveMultipleNotificationStatusesWithDifferentNames() {
    NotificationStatusModel status1 = new NotificationStatusModel();
    status1.setName("SUCCESS");

    NotificationStatusModel status2 = new NotificationStatusModel();
    status2.setName("PENDING");

    NotificationStatusModel status3 = new NotificationStatusModel();
    status3.setName("FAILURE");

    notificationStatusRepository.save(status1);
    notificationStatusRepository.save(status2);
    notificationStatusRepository.save(status3);

    List<NotificationStatusModel> allStatuses = notificationStatusRepository.findAll();

    assertThat(allStatuses).hasSize(3);
    assertThat(allStatuses)
        .extracting(NotificationStatusModel::getName)
        .containsExactlyInAnyOrder("SUCCESS", "PENDING", "FAILURE");
  }
}
