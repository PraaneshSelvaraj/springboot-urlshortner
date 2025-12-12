package com.example.repository;

import com.example.model.NotificationTypeModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTypeRepository extends JpaRepository<NotificationTypeModel, Long> {
  Optional<NotificationTypeModel> findByName(String name);
}
