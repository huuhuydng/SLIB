package slib.com.example.dto.kiosk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ValidateQrRequest {
    @NotBlank(message = "QR payload không được để trống")
    @Size(max = 4096, message = "QR payload không được vượt quá 4096 ký tự")
    private String qrPayload;

    @NotBlank(message = "Mã kiosk không được để trống")
    @Size(max = 50, message = "Mã kiosk không được vượt quá 50 ký tự")
    private String kioskCode;
}
