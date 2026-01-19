package com.example.controller;

import com.example.dto.CreateUserDto;
import com.example.dto.PagedUsersDto;
import com.example.dto.UserDto;
import com.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/api/users")
  public ResponseEntity<UserDto> createUser(@RequestBody CreateUserDto createUserDto) {
    UserDto response = userService.createUser(createUserDto);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @GetMapping("/api/users")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PagedUsersDto> getUsers(
      @RequestParam(defaultValue = "0") int pageNo,
      @RequestParam(defaultValue = "10") int pageSize,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortDirection) {
    PagedUsersDto pagedUsersDto = userService.getUsers(pageNo, pageSize, sortBy, sortDirection);
    return new ResponseEntity<>(pagedUsersDto, HttpStatus.OK);
  }

  @GetMapping("/api/users/{id}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<UserDto> getUserById(@PathVariable long id) {
    UserDto response = userService.getUserById(id);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @DeleteMapping("/api/users/{id}")
  @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
  public ResponseEntity<Void> deleteUserById(@PathVariable long id) {
    userService.deleteUserById(id);
    return ResponseEntity.noContent().build();
  }
}
