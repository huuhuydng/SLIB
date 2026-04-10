package slib.com.example.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualReputationAdjustmentRequest(
        @NotNull(message = "Số điểm điều chỉnh là bắt buộc")
        Integer points,

        @NotBlank(message = "Lý do điều chỉnh là bắt buộc")
        @Size(max = 500, message = "Lý do điều chỉnh không được vượt quá 500 ký tự")
        String reason) {
}
