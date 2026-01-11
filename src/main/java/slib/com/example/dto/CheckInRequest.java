package slib.com.example.dto;

import lombok.Data;

@Data
public class CheckInRequest {
    private String token;  
    private String gateId; 
}