package slib.com.example.dto.feedback;

import lombok.Data;

@Data
public class CreateViolationReportRequest {

    private Integer seatId;

    private String violationType;

    private String description;
}
