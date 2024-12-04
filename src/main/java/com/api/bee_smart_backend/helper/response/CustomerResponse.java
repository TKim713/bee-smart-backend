package com.api.bee_smart_backend.helper.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CustomerResponse {
    private String customerId;
    private String fullName;
    private String district;
    private String city;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;

    private String grade;
    private String className;
    private String school;
}
