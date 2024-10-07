package com.api.bee_smart_backend.model;

import com.api.bee_smart_backend.helper.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long user_id;

    @NonNull
    private String username;

    @NonNull
    private String email;

    @NonNull
    private String password;

    @NonNull
    private Role role;

    @NonNull
    private Timestamp create_at;
    private Timestamp update_at;
    private Timestamp delete_at;
}
