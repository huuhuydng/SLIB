package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateKioskRequest {
    @Size(max = 255, message = "Tên kiosk không được vượt quá 255 ký tự")
    private String kioskName;

    @Pattern(regexp = "^$|^(INTERACTIVE|MONITORING)$", message = "Loại kiosk phải là INTERACTIVE hoặc MONITORING")
    private String kioskType;

    @Size(max = 255, message = "Vị trí không được vượt quá 255 ký tự")
    private String location;

    private Boolean isActive;
}
