package slib.com.example.dto.users;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String dob; // yyyy-MM-dd format
}
