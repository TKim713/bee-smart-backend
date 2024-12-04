package com.api.bee_smart_backend.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customer")
public class Student extends Customer {
    private String grade;
    @DBRef
    private Parent parent;
    private String className;
    private String school;
}
