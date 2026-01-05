package com.example.dto;

import java.util.List;
import lombok.Data;

@Data
public class PagedUsersDto {
  private List<UserDto> users;
  private int pageNo;
  private int pageSize;
  private int totalPages;
  private long totalElements;
}
