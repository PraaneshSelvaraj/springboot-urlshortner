package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.model.NotificationTypeModel;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
@DisplayName("NotificationTypeRepository Tests")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class NotificationTypeRepositoryTest {

  private final TestEntityManager entityManager;
  private final NotificationTypeRepository notificationTypeRepository;

  @Autowired
  public NotificationTypeRepositoryTest(
      TestEntityManager entityManager, NotificationTypeRepository notificationTypeRepository) {
    this.entityManager = entityManager;
    this.notificationTypeRepository = notificationTypeRepository;
  }

  @Test
  @DisplayName("Should save and retrieve notification type successfully")
  void shouldSaveAndRetrieveNotificationTypeSuccessfully() {
    NotificationTypeModel type = new NotificationTypeModel();
    type.setName("NEWURL");

    NotificationTypeModel saved = notificationTypeRepository.save(type);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("NEWURL");
  }

  @Test
  @DisplayName("Should find notification type by name")
  void shouldFindNotificationTypeByName() {
    NotificationTypeModel type = new NotificationTypeModel();
    type.setName("THRESHOLD");
    entityManager.persistAndFlush(type);

    Optional<NotificationTypeModel> found = notificationTypeRepository.findByName("THRESHOLD");

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("THRESHOLD");
  }

  @Test
  @DisplayName("Should return empty when notification type not found by name")
  void shouldReturnEmptyWhenNotificationTypeNotFoundByName() {
    Optional<NotificationTypeModel> found = notificationTypeRepository.findByName("NONEXISTENT");

    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find notification type by id")
  void shouldFindNotificationTypeById() {
    NotificationTypeModel type = new NotificationTypeModel();
    type.setName("NEWUSER");

    NotificationTypeModel saved = entityManager.persistAndFlush(type);

    Optional<NotificationTypeModel> found =
        notificationTypeRepository.findById(saved.getId().longValue());

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo(saved.getId());
    assertThat(found.get().getName()).isEqualTo("NEWUSER");
  }

  @Test
  @DisplayName("Should find all notification types")
  void shouldFindAllNotificationTypes() {
    NotificationTypeModel type1 = new NotificationTypeModel();
    type1.setName("NEWURL");

    NotificationTypeModel type2 = new NotificationTypeModel();
    type2.setName("THRESHOLD");

    entityManager.persistAndFlush(type1);
    entityManager.persistAndFlush(type2);

    List<NotificationTypeModel> types = notificationTypeRepository.findAll();

    assertThat(types).hasSize(2);
    assertThat(types).extracting(NotificationTypeModel::getName).contains("NEWURL", "THRESHOLD");
  }

  @Test
  @DisplayName("Should delete notification type")
  void shouldDeleteNotificationType() {
    NotificationTypeModel type = new NotificationTypeModel();
    type.setName("TO_DELETE");

    NotificationTypeModel saved = entityManager.persistAndFlush(type);
    Integer id = saved.getId();

    notificationTypeRepository.delete(saved);
    entityManager.flush();

    Optional<NotificationTypeModel> found = notificationTypeRepository.findById(id.longValue());
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should count notification types")
  void shouldCountNotificationTypes() {
    NotificationTypeModel type1 = new NotificationTypeModel();
    type1.setName("TYPE1");

    NotificationTypeModel type2 = new NotificationTypeModel();
    type2.setName("TYPE2");

    entityManager.persistAndFlush(type1);
    entityManager.persistAndFlush(type2);

    long count = notificationTypeRepository.count();

    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("Should update notification type name")
  void shouldUpdateNotificationTypeName() {
    NotificationTypeModel type = new NotificationTypeModel();
    type.setName("ORIGINAL");

    NotificationTypeModel saved = entityManager.persistAndFlush(type);
    Integer id = saved.getId();

    saved.setName("UPDATED");
    notificationTypeRepository.save(saved);
    entityManager.flush();
    entityManager.clear();

    NotificationTypeModel updated =
        notificationTypeRepository.findById(id.longValue()).orElseThrow();
    assertThat(updated.getName()).isEqualTo("UPDATED");
  }

  @Test
  @DisplayName("Should handle case-sensitive name search")
  void shouldHandleCaseSensitiveNameSearch() {
    NotificationTypeModel type = new NotificationTypeModel();
    type.setName("NEWURL");
    entityManager.persistAndFlush(type);

    Optional<NotificationTypeModel> foundUpper = notificationTypeRepository.findByName("NEWURL");
    Optional<NotificationTypeModel> foundLower = notificationTypeRepository.findByName("newurl");

    assertThat(foundUpper).isPresent();
    assertThat(foundLower).isEmpty();
  }

  @Test
  @DisplayName("Should return empty list when no types exist")
  void shouldReturnEmptyListWhenNoTypesExist() {
    List<NotificationTypeModel> types = notificationTypeRepository.findAll();

    assertThat(types).isEmpty();
  }

  @Test
  @DisplayName("Should save multiple notification types with different names")
  void shouldSaveMultipleNotificationTypesWithDifferentNames() {
    NotificationTypeModel type1 = new NotificationTypeModel();
    type1.setName("NEWURL");

    NotificationTypeModel type2 = new NotificationTypeModel();
    type2.setName("THRESHOLD");

    NotificationTypeModel type3 = new NotificationTypeModel();
    type3.setName("NEWUSER");

    notificationTypeRepository.save(type1);
    notificationTypeRepository.save(type2);
    notificationTypeRepository.save(type3);

    List<NotificationTypeModel> allTypes = notificationTypeRepository.findAll();

    assertThat(allTypes).hasSize(3);
    assertThat(allTypes)
        .extracting(NotificationTypeModel::getName)
        .containsExactlyInAnyOrder("NEWURL", "THRESHOLD", "NEWUSER");
  }
}
