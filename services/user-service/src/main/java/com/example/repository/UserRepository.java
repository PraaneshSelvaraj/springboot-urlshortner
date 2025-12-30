package com.example.repository;

import com.example.model.UserModel;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserModel, Long> {
  Optional<UserModel> findByEmail(String email);

  @Modifying
  @Transactional
  @Query("UPDATE UserModel u SET u.isDeleted = true WHERE u.id = :id")
  int softDeleteById(@Param("id") Long id);

  @Modifying
  @Transactional
  @Query("UPDATE UserModel u SET u.refreshToken = :refreshToken WHERE u.id = :id")
  int updateRefreshToken(@Param("id") Long id, @Param("refreshToken") String refreshToken);
}
