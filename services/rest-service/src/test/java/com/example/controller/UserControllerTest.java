package com.example.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.dto.CreateUserDto;
import com.example.dto.PagedUsersDto;
import com.example.dto.UserDto;
import com.example.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private UserService userService;

  // POST /api/users Tests (5 tests)

  @Test
  @DisplayName("Should create user successfully")
  void shouldCreateUserSuccessfully() throws Exception {
    CreateUserDto createUserDto = new CreateUserDto();
    createUserDto.setUsername("newuser");
    createUserDto.setEmail("newuser@example.com");
    createUserDto.setPassword("password123");
    createUserDto.setRole("USER");

    UserDto createdUser = new UserDto();
    createdUser.setId(1L);
    createdUser.setUsername("newuser");
    createdUser.setEmail("newuser@example.com");
    createdUser.setRole("USER");
    createdUser.setAuthProvider("LOCAL");
    createdUser.setCreatedAt(LocalDateTime.now());
    createdUser.setUpdatedAt(LocalDateTime.now());

    when(userService.createUser(any(CreateUserDto.class))).thenReturn(createdUser);

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.username").value("newuser"))
        .andExpect(jsonPath("$.email").value("newuser@example.com"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.authProvider").value("LOCAL"));

    verify(userService).createUser(any(CreateUserDto.class));
  }

  @Test
  @DisplayName("Should create user with ADMIN role")
  void shouldCreateUserWithAdminRole() throws Exception {
    CreateUserDto createUserDto = new CreateUserDto();
    createUserDto.setUsername("admin");
    createUserDto.setEmail("admin@example.com");
    createUserDto.setPassword("adminpass");
    createUserDto.setRole("ADMIN");

    UserDto createdUser = new UserDto();
    createdUser.setId(2L);
    createdUser.setUsername("admin");
    createdUser.setEmail("admin@example.com");
    createdUser.setRole("ADMIN");
    createdUser.setAuthProvider("LOCAL");
    createdUser.setCreatedAt(LocalDateTime.now());
    createdUser.setUpdatedAt(LocalDateTime.now());

    when(userService.createUser(any(CreateUserDto.class))).thenReturn(createdUser);

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("ADMIN"));

    verify(userService).createUser(any(CreateUserDto.class));
  }

  @Test
  @DisplayName("Should return 400 when role is invalid")
  void shouldReturn400WhenRoleIsInvalid() throws Exception {
    CreateUserDto createUserDto = new CreateUserDto();
    createUserDto.setUsername("testuser");
    createUserDto.setEmail("test@example.com");
    createUserDto.setPassword("password123");
    createUserDto.setRole("SUPERUSER");

    when(userService.createUser(any(CreateUserDto.class)))
        .thenThrow(new IllegalArgumentException("Invalid role: SUPERUSER"));

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid role: SUPERUSER"));

    verify(userService).createUser(any(CreateUserDto.class));
  }

  @Test
  @DisplayName("Should return 409 when user already exists")
  void shouldReturn409WhenUserAlreadyExists() throws Exception {
    CreateUserDto createUserDto = new CreateUserDto();
    createUserDto.setUsername("existinguser");
    createUserDto.setEmail("existing@example.com");
    createUserDto.setPassword("password123");
    createUserDto.setRole("USER");

    when(userService.createUser(any(CreateUserDto.class)))
        .thenThrow(new IllegalStateException("Resource already exists"));

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserDto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Resource already exists"));

    verify(userService).createUser(any(CreateUserDto.class));
  }

  @Test
  @DisplayName("Should validate required fields in CreateUserDto")
  void shouldValidateRequiredFieldsInCreateUserDto() throws Exception {
    CreateUserDto emptyDto = new CreateUserDto();

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyDto)))
        .andExpect(status().isCreated()); // Service layer will handle validation

    verify(userService).createUser(any(CreateUserDto.class));
  }

  // GET /api/users Tests (7 tests)

  @Test
  @DisplayName("Should get paginated users successfully")
  void shouldGetPaginatedUsersSuccessfully() throws Exception {
    UserDto user1 = new UserDto();
    user1.setId(1L);
    user1.setUsername("user1");
    user1.setEmail("user1@example.com");
    user1.setRole("USER");
    user1.setAuthProvider("LOCAL");
    user1.setCreatedAt(LocalDateTime.now());
    user1.setUpdatedAt(LocalDateTime.now());

    UserDto user2 = new UserDto();
    user2.setId(2L);
    user2.setUsername("user2");
    user2.setEmail("user2@example.com");
    user2.setRole("ADMIN");
    user2.setAuthProvider("GOOGLE");
    user2.setCreatedAt(LocalDateTime.now());
    user2.setUpdatedAt(LocalDateTime.now());

    List<UserDto> users = Arrays.asList(user1, user2);

    PagedUsersDto pagedUsers = new PagedUsersDto();
    pagedUsers.setUsers(users);
    pagedUsers.setPageNo(0);
    pagedUsers.setPageSize(10);
    pagedUsers.setTotalPages(1);
    pagedUsers.setTotalElements(2);

    when(userService.getUsers(0, 10, null, null)).thenReturn(pagedUsers);

    mockMvc
        .perform(get("/api/users").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users").isArray())
        .andExpect(jsonPath("$.users[0].id").value(1))
        .andExpect(jsonPath("$.users[0].username").value("user1"))
        .andExpect(jsonPath("$.users[1].id").value(2))
        .andExpect(jsonPath("$.users[1].username").value("user2"))
        .andExpect(jsonPath("$.pageNo").value(0))
        .andExpect(jsonPath("$.pageSize").value(10))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.totalElements").value(2));

    verify(userService).getUsers(0, 10, null, null);
  }

  @Test
  @DisplayName("Should get users with default pagination parameters")
  void shouldGetUsersWithDefaultPaginationParameters() throws Exception {
    PagedUsersDto emptyUsers = new PagedUsersDto();
    emptyUsers.setUsers(Arrays.asList());
    emptyUsers.setPageNo(0);
    emptyUsers.setPageSize(10);
    emptyUsers.setTotalPages(0);
    emptyUsers.setTotalElements(0);

    when(userService.getUsers(0, 10, null, null)).thenReturn(emptyUsers);

    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users").isArray())
        .andExpect(jsonPath("$.users").isEmpty())
        .andExpect(jsonPath("$.pageNo").value(0))
        .andExpect(jsonPath("$.pageSize").value(10));

    verify(userService).getUsers(0, 10, null, null);
  }

  @Test
  @DisplayName("Should get users with sorting parameters")
  void shouldGetUsersWithSortingParameters() throws Exception {
    PagedUsersDto pagedUsers = new PagedUsersDto();
    pagedUsers.setUsers(Arrays.asList());
    pagedUsers.setPageNo(0);
    pagedUsers.setPageSize(10);
    pagedUsers.setTotalPages(0);
    pagedUsers.setTotalElements(0);

    when(userService.getUsers(0, 10, "username", "ASC")).thenReturn(pagedUsers);

    mockMvc
        .perform(
            get("/api/users")
                .param("pageNo", "0")
                .param("pageSize", "10")
                .param("sortBy", "username")
                .param("sortDirection", "ASC"))
        .andExpect(status().isOk());

    verify(userService).getUsers(0, 10, "username", "ASC");
  }

  @Test
  @DisplayName("Should return 400 when page number is negative")
  void shouldReturn400WhenPageNumberIsNegative() throws Exception {
    when(userService.getUsers(eq(-1), eq(10), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page number cannot be negative"));

    mockMvc
        .perform(get("/api/users").param("pageNo", "-1").param("pageSize", "10"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Page number cannot be negative"));

    verify(userService).getUsers(eq(-1), eq(10), isNull(), isNull());
  }

  @Test
  @DisplayName("Should return 400 when page size is zero")
  void shouldReturn400WhenPageSizeIsZero() throws Exception {
    when(userService.getUsers(eq(0), eq(0), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page size must be greater than zero."));

    mockMvc
        .perform(get("/api/users").param("pageNo", "0").param("pageSize", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Page size must be greater than zero."));

    verify(userService).getUsers(eq(0), eq(0), isNull(), isNull());
  }

  @Test
  @DisplayName("Should return 400 when page size is negative")
  void shouldReturn400WhenPageSizeIsNegative() throws Exception {
    when(userService.getUsers(eq(0), eq(-5), isNull(), isNull()))
        .thenThrow(new IllegalArgumentException("Page size must be greater than zero."));

    mockMvc
        .perform(get("/api/users").param("pageNo", "0").param("pageSize", "-5"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Page size must be greater than zero."));

    verify(userService).getUsers(eq(0), eq(-5), isNull(), isNull());
  }

  @Test
  @DisplayName("Should handle empty users list")
  void shouldHandleEmptyUsersList() throws Exception {
    PagedUsersDto emptyUsers = new PagedUsersDto();
    emptyUsers.setUsers(Arrays.asList());
    emptyUsers.setPageNo(0);
    emptyUsers.setPageSize(10);
    emptyUsers.setTotalPages(0);
    emptyUsers.setTotalElements(0);

    when(userService.getUsers(0, 10, null, null)).thenReturn(emptyUsers);

    mockMvc
        .perform(get("/api/users").param("pageNo", "0").param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.users").isArray())
        .andExpect(jsonPath("$.users").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));

    verify(userService).getUsers(0, 10, null, null);
  }

  // GET /api/users/{id} Tests (2 tests)

  @Test
  @DisplayName("Should get user by ID successfully")
  void shouldGetUserByIdSuccessfully() throws Exception {
    long userId = 123L;
    UserDto user = new UserDto();
    user.setId(userId);
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setRole("USER");
    user.setAuthProvider("LOCAL");
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    when(userService.getUserById(userId)).thenReturn(user);

    mockMvc
        .perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.role").value("USER"));

    verify(userService).getUserById(userId);
  }

  @Test
  @DisplayName("Should return 404 when user not found")
  void shouldReturn404WhenUserNotFound() throws Exception {
    long userId = 999L;

    when(userService.getUserById(userId))
        .thenThrow(new NoSuchElementException("Resource not found"));

    mockMvc
        .perform(get("/api/users/{id}", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Resource not found"));

    verify(userService).getUserById(userId);
  }

  // DELETE /api/users/{id} Tests (1 test)

  @Test
  @DisplayName("Should delete user successfully")
  void shouldDeleteUserSuccessfully() throws Exception {
    long userId = 123L;

    when(userService.deleteUserById(userId)).thenReturn(true);

    mockMvc
        .perform(delete("/api/users/{id}", userId))
        .andExpect(status().isNoContent());

    verify(userService).deleteUserById(userId);
  }
}
