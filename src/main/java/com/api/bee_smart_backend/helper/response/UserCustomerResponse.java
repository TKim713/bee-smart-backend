package com.api.bee_smart_backend.helper.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserCustomerResponse {
    private String userId;
    private String fullName;
    private String username;
    private String role;
    private String district;
    private String city;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateOfBirth;
    private String phone;
    private String email;
    private String address;

    private String grade;
    private String className;
    private String school;
}
