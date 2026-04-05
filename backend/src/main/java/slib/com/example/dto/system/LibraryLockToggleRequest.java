package slib.com.example.dto.system;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryLockToggleRequest {
    private Boolean closed;

    @Size(max = 500, message = "Lý do đóng thư viện không được vượt quá 500 ký tự")
    private String reason;
}
