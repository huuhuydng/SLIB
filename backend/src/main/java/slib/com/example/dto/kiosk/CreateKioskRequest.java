package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKioskRequest {
    @NotBlank(message = "Mã kiosk không được để trống")
    @Size(max = 50, message = "Mã kiosk không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã kiosk chỉ được chứa chữ cái, số, gạch ngang hoặc gạch dưới")
    private String kioskCode;

    @NotBlank(message = "Tên kiosk không được để trống")
    @Size(max = 255, message = "Tên kiosk không được vượt quá 255 ký tự")
    private String kioskName;

    @NotBlank(message = "Loại kiosk không được để trống")
    @Pattern(regexp = "^(INTERACTIVE|MONITORING)$", message = "Loại kiosk phải là INTERACTIVE hoặc MONITORING")
    private String kioskType;

    @Size(max = 255, message = "Vị trí không được vượt quá 255 ký tự")
    private String location;
}
