package com.api.bee_smart_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer")
public abstract class Customer {
    @Id
    private String customerId;
    private String fullName;
    private String district;
    private String city;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;

    @DBRef(lazy = true)
    private User user;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
    @LastModifiedDate
    private Instant deletedAt;
}
