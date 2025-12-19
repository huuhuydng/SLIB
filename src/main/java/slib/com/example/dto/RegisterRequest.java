package slib.com.example.dto;
import java.time.LocalDate;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
    private String studentCode;
    private LocalDate dob;
}
