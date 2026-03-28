package slib.com.example.dto.feedback;

import lombok.Data;

@Data
public class CreateSeatStatusReportRequest {
    private Integer seatId;
    private String issueType;
    private String description;
}
