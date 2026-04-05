package slib.com.example.dto.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateComplaintRequest {
    @NotBlank(message = "Tiêu đề khiếu nại không được để trống")
    @Size(max = 255, message = "Tiêu đề khiếu nại không được vượt quá 255 ký tự")
    private String subject;

    @NotBlank(message = "Nội dung khiếu nại không được để trống")
    @Size(max = 4000, message = "Nội dung khiếu nại không được vượt quá 4000 ký tự")
    private String content;

    @Size(max = 1000, message = "Đường dẫn minh chứng không được vượt quá 1000 ký tự")
    private String evidenceUrl;

    private UUID pointTransactionId;
    private UUID violationReportId;
}
