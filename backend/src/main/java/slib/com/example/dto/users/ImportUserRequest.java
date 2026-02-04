package slib.com.example.dto.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import slib.com.example.entity.users.Role;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportUserRequest {
    private String fullName;
    private String userCode;
    private String email;
    private String phone;
    private LocalDate dob;
    private Role role;
    private String avtUrl; // Avatar URL from Cloudinary
}
