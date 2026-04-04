package slib.com.example.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.users.Role;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateUserRequest {
    private String fullName;
    private String email;
    private String userCode;
    private String phone;
    private LocalDate dob;
    private Role role;
}
