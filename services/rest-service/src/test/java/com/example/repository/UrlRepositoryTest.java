package com.example.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.model.Url;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(locations = "classpath:application.properties")
@DisplayName("UrlRepository Tests")
class UrlRepositoryTest {

  private final TestEntityManager entityManager;
  private final UrlRepository urlRepository;

  private Url testUrl;

  @Autowired
  public UrlRepositoryTest(TestEntityManager entityManager, UrlRepository urlRepository) {
    this.entityManager = entityManager;
    this.urlRepository = urlRepository;
  }

  @BeforeEach
  void setUp() {
    testUrl = new Url();
    testUrl.setShortCode("abc123");
    testUrl.setLongUrl("https://www.example.com/very-long-url");
    testUrl.setClicks(0);
    testUrl.setDeleted(false);
    testUrl.setCreatedBy(1L);
    testUrl.setCreatedAt(LocalDateTime.now());
    testUrl.setUpdatedAt(LocalDateTime.now());
    testUrl.setExpiresAt(LocalDateTime.now().plusDays(30));
  }

  @Test
  @DisplayName("Should save and retrieve URL successfully")
  void shouldSaveAndRetrieveUrl() {
    Url savedUrl = urlRepository.save(testUrl);
    entityManager.flush();

    assertThat(savedUrl).isNotNull();
    assertThat(savedUrl.getId()).isNotNull();
    assertThat(savedUrl.getShortCode()).isEqualTo("abc123");
    assertThat(savedUrl.getLongUrl()).isEqualTo("https://www.example.com/very-long-url");
    assertThat(savedUrl.getClicks()).isEqualTo(0);
    assertThat(savedUrl.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("Should find URL by short code when exists")
  void shouldFindByShortCodeWhenExists() {
    entityManager.persistAndFlush(testUrl);

    Optional<Url> foundUrl = urlRepository.findByShortCode("abc123");

    assertThat(foundUrl).isPresent();
    assertThat(foundUrl.get().getShortCode()).isEqualTo("abc123");
    assertThat(foundUrl.get().getLongUrl()).isEqualTo("https://www.example.com/very-long-url");
  }

  @Test
  @DisplayName("Should return empty optional when short code does not exist")
  void shouldReturnEmptyWhenShortCodeDoesNotExist() {
    Optional<Url> foundUrl = urlRepository.findByShortCode("notexists");

    assertThat(foundUrl).isEmpty();
  }

  @Test
  @DisplayName("Should increment click count successfully")
  void shouldIncrementClickCount() {
    entityManager.persistAndFlush(testUrl);
    assertThat(testUrl.getClicks()).isEqualTo(0);

    urlRepository.incrementClickCount("abc123");
    entityManager.flush();
    entityManager.clear();

    Url updatedUrl = urlRepository.findByShortCode("abc123").orElseThrow();
    assertThat(updatedUrl.getClicks()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should increment click count multiple times")
  void shouldIncrementClickCountMultipleTimes() {
    entityManager.persistAndFlush(testUrl);

    urlRepository.incrementClickCount("abc123");
    urlRepository.incrementClickCount("abc123");
    urlRepository.incrementClickCount("abc123");
    entityManager.flush();
    entityManager.clear();

    Url updatedUrl = urlRepository.findByShortCode("abc123").orElseThrow();
    assertThat(updatedUrl.getClicks()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should get click count for existing short code")
  void shouldGetClickCount() {
    testUrl.setClicks(5);
    entityManager.persistAndFlush(testUrl);

    Integer clickCount = urlRepository.getClickCount("abc123");

    assertThat(clickCount).isEqualTo(5);
  }

  @Test
  @DisplayName("Should return null when getting click count for non-existent short code")
  void shouldReturnNullForNonExistentShortCode() {
    Integer clickCount = urlRepository.getClickCount("notexists");

    assertThat(clickCount).isNull();
  }

  @Test
  @DisplayName("Should update click count and retrieve it correctly")
  void shouldUpdateAndRetrieveClickCount() {
    entityManager.persistAndFlush(testUrl);

    urlRepository.incrementClickCount("abc123");
    urlRepository.incrementClickCount("abc123");
    entityManager.flush();
    Integer clickCount = urlRepository.getClickCount("abc123");

    assertThat(clickCount).isEqualTo(2);
  }

  @Test
  @DisplayName("Should enforce unique constraint on short code")
  void shouldEnforceUniqueShortCode() {
    entityManager.persistAndFlush(testUrl);

    Url duplicateUrl = new Url();
    duplicateUrl.setShortCode("abc123");
    duplicateUrl.setLongUrl("https://www.different.com");
    duplicateUrl.setClicks(0);
    duplicateUrl.setDeleted(false);
    duplicateUrl.setCreatedAt(LocalDateTime.now());
    duplicateUrl.setUpdatedAt(LocalDateTime.now());

    assertThat(assertThrows(Exception.class, () -> entityManager.persistAndFlush(duplicateUrl)))
        .isNotNull();
  }

  @Test
  @DisplayName("Should delete URL by ID")
  void shouldDeleteUrlById() {
    Url savedUrl = entityManager.persistAndFlush(testUrl);
    Long urlId = savedUrl.getId();

    urlRepository.deleteById(urlId);
    entityManager.flush();

    Optional<Url> deletedUrl = urlRepository.findById(urlId);
    assertThat(deletedUrl).isEmpty();
  }

  @Test
  @DisplayName("Should find all URLs")
  void shouldFindAllUrls() {
    Url url1 = new Url();
    url1.setShortCode("abc123");
    url1.setLongUrl("https://www.example1.com");
    url1.setClicks(0);
    url1.setDeleted(false);
    url1.setCreatedAt(LocalDateTime.now());
    url1.setUpdatedAt(LocalDateTime.now());

    Url url2 = new Url();
    url2.setShortCode("xyz789");
    url2.setLongUrl("https://www.example2.com");
    url2.setClicks(0);
    url2.setDeleted(false);
    url2.setCreatedAt(LocalDateTime.now());
    url2.setUpdatedAt(LocalDateTime.now());

    entityManager.persist(url1);
    entityManager.persist(url2);
    entityManager.flush();

    var allUrls = urlRepository.findAll();

    assertThat(allUrls).hasSize(2);
    assertThat(allUrls).extracting(Url::getShortCode).containsExactlyInAnyOrder("abc123", "xyz789");
  }

  @Test
  @DisplayName("Should handle URLs with null expiration date")
  void shouldHandleNullExpirationDate() {
    testUrl.setExpiresAt(null);

    Url savedUrl = urlRepository.save(testUrl);
    entityManager.flush();

    assertThat(savedUrl.getExpiresAt()).isNull();
  }

  @Test
  @DisplayName("Should update existing URL")
  void shouldUpdateExistingUrl() {
    Url savedUrl = entityManager.persistAndFlush(testUrl);
    Long urlId = savedUrl.getId();

    savedUrl.setLongUrl("https://www.updated-url.com");
    savedUrl.setUpdatedAt(LocalDateTime.now());
    urlRepository.save(savedUrl);
    entityManager.flush();
    entityManager.clear();

    Url updatedUrl = urlRepository.findById(urlId).orElseThrow();
    assertThat(updatedUrl.getLongUrl()).isEqualTo("https://www.updated-url.com");
  }

  @Test
  @DisplayName("Should count total URLs in repository")
  void shouldCountTotalUrls() {
    entityManager.persistAndFlush(testUrl);

    Url url2 = new Url();
    url2.setShortCode("xyz789");
    url2.setLongUrl("https://www.example2.com");
    url2.setClicks(0);
    url2.setDeleted(false);
    url2.setCreatedAt(LocalDateTime.now());
    url2.setUpdatedAt(LocalDateTime.now());
    entityManager.persistAndFlush(url2);

    long count = urlRepository.count();

    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("Should save URL with createdBy field")
  void shouldSaveUrlWithCreatedByField() {
    testUrl.setCreatedBy(123L);

    Url savedUrl = urlRepository.save(testUrl);
    entityManager.flush();

    assertThat(savedUrl).isNotNull();
    assertThat(savedUrl.getCreatedBy()).isEqualTo(123L);
  }

  @Test
  @DisplayName("Should find URLs by createdBy user ID")
  void shouldFindUrlsByCreatedBy() {
    Long userId = 100L;

    Url url1 = new Url();
    url1.setShortCode("user100-1");
    url1.setLongUrl("https://www.example1.com");
    url1.setClicks(0);
    url1.setDeleted(false);
    url1.setCreatedBy(userId);
    url1.setCreatedAt(LocalDateTime.now());
    url1.setUpdatedAt(LocalDateTime.now());

    Url url2 = new Url();
    url2.setShortCode("user100-2");
    url2.setLongUrl("https://www.example2.com");
    url2.setClicks(0);
    url2.setDeleted(false);
    url2.setCreatedBy(userId);
    url2.setCreatedAt(LocalDateTime.now());
    url2.setUpdatedAt(LocalDateTime.now());

    Url url3 = new Url();
    url3.setShortCode("user200-1");
    url3.setLongUrl("https://www.example3.com");
    url3.setClicks(0);
    url3.setDeleted(false);
    url3.setCreatedBy(200L);
    url3.setCreatedAt(LocalDateTime.now());
    url3.setUpdatedAt(LocalDateTime.now());

    entityManager.persist(url1);
    entityManager.persist(url2);
    entityManager.persist(url3);
    entityManager.flush();

    Pageable pageable = PageRequest.of(0, 10);
    Page<Url> userUrls = urlRepository.findByCreatedBy(userId, pageable);

    assertThat(userUrls).isNotNull();
    assertThat(userUrls.getContent()).hasSize(2);
    assertThat(userUrls.getContent())
        .extracting(Url::getCreatedBy)
        .containsOnly(userId);
    assertThat(userUrls.getContent())
        .extracting(Url::getShortCode)
        .containsExactlyInAnyOrder("user100-1", "user100-2");
  }

  @Test
  @DisplayName("Should return empty page when no URLs exist for user")
  void shouldReturnEmptyPageWhenNoUrlsExistForUser() {
    Long userId = 999L;

    Url url1 = new Url();
    url1.setShortCode("other-user");
    url1.setLongUrl("https://www.example1.com");
    url1.setClicks(0);
    url1.setDeleted(false);
    url1.setCreatedBy(100L);
    url1.setCreatedAt(LocalDateTime.now());
    url1.setUpdatedAt(LocalDateTime.now());

    entityManager.persistAndFlush(url1);

    Pageable pageable = PageRequest.of(0, 10);
    Page<Url> userUrls = urlRepository.findByCreatedBy(userId, pageable);

    assertThat(userUrls).isNotNull();
    assertThat(userUrls.getContent()).isEmpty();
    assertThat(userUrls.getTotalElements()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should paginate URLs by createdBy correctly")
  void shouldPaginateUrlsByCreatedByCorrectly() {
    Long userId = 100L;

    for (int i = 1; i <= 5; i++) {
      Url url = new Url();
      url.setShortCode("url-" + i);
      url.setLongUrl("https://www.example" + i + ".com");
      url.setClicks(0);
      url.setDeleted(false);
      url.setCreatedBy(userId);
      url.setCreatedAt(LocalDateTime.now());
      url.setUpdatedAt(LocalDateTime.now());
      entityManager.persist(url);
    }
    entityManager.flush();

    Pageable firstPage = PageRequest.of(0, 2);
    Page<Url> firstPageResult = urlRepository.findByCreatedBy(userId, firstPage);

    assertThat(firstPageResult.getContent()).hasSize(2);
    assertThat(firstPageResult.getTotalElements()).isEqualTo(5);
    assertThat(firstPageResult.getTotalPages()).isEqualTo(3);

    Pageable secondPage = PageRequest.of(1, 2);
    Page<Url> secondPageResult = urlRepository.findByCreatedBy(userId, secondPage);

    assertThat(secondPageResult.getContent()).hasSize(2);
    assertThat(secondPageResult.getTotalElements()).isEqualTo(5);
  }

  @Test
  @DisplayName("Should allow null createdBy field")
  void shouldAllowNullCreatedByField() {
    testUrl.setCreatedBy(null);

    Url savedUrl = urlRepository.save(testUrl);
    entityManager.flush();

    assertThat(savedUrl).isNotNull();
    assertThat(savedUrl.getCreatedBy()).isNull();
  }
}
