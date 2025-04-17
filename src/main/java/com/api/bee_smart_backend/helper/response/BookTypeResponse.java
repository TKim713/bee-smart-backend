package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BookTypeResponse {
    private String bookId;
    private String bookName;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
