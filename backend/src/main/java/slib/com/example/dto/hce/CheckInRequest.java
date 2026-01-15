package slib.com.example.dto.hce;

import lombok.Data;

@Data
public class CheckInRequest {
    private String token;  
    private String gateId; 
}