package slib.com.example.dto.system;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestSystemReputationTargetRequest(
        @NotNull(message = "Điểm mục tiêu là bắt buộc")
        @Min(value = 0, message = "Điểm mục tiêu không được nhỏ hơn 0")
        @Max(value = 200, message = "Điểm mục tiêu không được lớn hơn 200")
        Integer targetScore,

        @NotBlank(message = "Lý do điều chỉnh là bắt buộc")
        @Size(max = 500, message = "Lý do điều chỉnh không được vượt quá 500 ký tự")
        String reason) {
}
