package com.example.urlshortner.repository;

import com.example.urlshortner.model.Url;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

  Optional<Url> findByShortCode(String shortCode);

  @Modifying
  @Transactional
  @Query(
      "UPDATE Url u SET u.clicks = u.clicks + 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.shortCode"
          + " = :shortCode")
  void incrementClickCount(@Param("shortCode") String shortCode);

  @Query("SELECT u.clicks FROM Url u WHERE u.shortCode = :shortCode")
  Integer getClickCount(@Param("shortCode") String shortCode);
}
