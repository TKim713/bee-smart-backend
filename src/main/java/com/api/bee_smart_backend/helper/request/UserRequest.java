package com.api.bee_smart_backend.helper.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Data
public abstract class UserRequest {
    private String fullName;
    private String district;
    private String city;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;
}
