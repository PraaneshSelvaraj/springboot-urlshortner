package com.example.repository;

import com.example.model.NotificationStatusModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationStatusRepository extends JpaRepository<NotificationStatusModel, Long> {
  Optional<NotificationStatusModel> findByName(String name);
}
