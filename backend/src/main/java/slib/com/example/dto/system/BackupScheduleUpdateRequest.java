package slib.com.example.dto.system;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BackupScheduleUpdateRequest {

    @NotBlank(message = "Thời gian sao lưu không được để trống")
    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$", message = "Thời gian sao lưu phải theo định dạng HH:mm")
    private String time;

    @NotNull(message = "Số ngày lưu trữ không được để trống")
    @Min(value = 1, message = "Số ngày lưu trữ phải từ 1 ngày trở lên")
    @Max(value = 365, message = "Số ngày lưu trữ không được vượt quá 365 ngày")
    private Integer retainDays;

    @NotNull(message = "Trạng thái lịch sao lưu không được để trống")
    private Boolean isActive;
}
