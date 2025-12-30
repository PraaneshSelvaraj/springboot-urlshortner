package com.example;

import com.example.service.GrpcUserService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.lang.Nullable;

@SpringBootApplication
@EnableJpaAuditing
public class UserApplication {

  private final GrpcUserService grpcService;

  @Value("${grpc.server.port}")
  private int grpcPort;

  public UserApplication(@Nullable GrpcUserService grpcService) {
    this.grpcService = grpcService;
  }

  public static void main(String[] args) {
    SpringApplication.run(UserApplication.class, args);
  }

  @PostConstruct
  public void startGrpcServer() throws IOException {
    if (grpcService != null && grpcPort > 0) {
      Server server = ServerBuilder.forPort(grpcPort).addService(grpcService).build();
      System.out.println("gRPC Server started on port " + grpcPort);
      server.start();
    }
  }
}
