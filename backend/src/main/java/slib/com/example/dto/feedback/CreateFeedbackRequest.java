package slib.com.example.dto.feedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateFeedbackRequest {
    @NotNull(message = "Đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao")
    private Integer rating;

    @Size(max = 2000, message = "Nội dung phản hồi không được vượt quá 2000 ký tự")
    private String content;

    @Pattern(
            regexp = "^$|^(FACILITY|SERVICE|GENERAL|MESSAGE)$",
            message = "Loại phản hồi không hợp lệ")
    private String category;

    private UUID conversationId;
    private UUID reservationId;
}
