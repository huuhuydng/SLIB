package slib.com.example.dto.ai;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIConfigRequest {

    @NotBlank(message = "Vui lòng chọn nhà cung cấp AI")
    @Pattern(regexp = "^(ollama|gemini)$", message = "Nhà cung cấp AI chỉ được là 'ollama' hoặc 'gemini'")
    private String provider;

    @Size(max = 50, message = "Tên mô hình Ollama không được vượt quá 50 ký tự")
    private String ollamaModel;

    @Size(max = 200, message = "Địa chỉ Ollama không được vượt quá 200 ký tự")
    private String ollamaUrl;

    @Size(max = 500, message = "API key không được vượt quá 500 ký tự")
    private String apiKey;

    @Size(max = 50, message = "Tên mô hình Gemini không được vượt quá 50 ký tự")
    private String geminiModel;

    @NotNull(message = "Vui lòng nhập nhiệt độ sinh câu trả lời")
    @DecimalMin(value = "0.0", message = "Nhiệt độ phải từ 0.0 trở lên")
    @DecimalMax(value = "2.0", message = "Nhiệt độ không được vượt quá 2.0")
    private Double temperature;

    @NotNull(message = "Vui lòng nhập số token tối đa")
    @Min(value = 1, message = "Số token tối đa phải lớn hơn 0")
    @Max(value = 8192, message = "Số token tối đa không được vượt quá 8192")
    private Integer maxTokens;

    @Size(max = 10000, message = "System prompt không được vượt quá 10000 ký tự")
    private String systemPrompt;

    @NotNull(message = "Thiếu cấu hình ngữ cảnh")
    private Boolean enableContext;

    @NotNull(message = "Thiếu cấu hình lịch sử hội thoại")
    private Boolean enableHistory;

    @NotNull(message = "Thiếu cấu hình gợi ý tự động")
    private Boolean autoSuggest;

    @NotBlank(message = "Vui lòng chọn ngôn ngữ phản hồi")
    @Pattern(regexp = "^(vi|en)$", message = "Ngôn ngữ phản hồi chỉ được là 'vi' hoặc 'en'")
    private String responseLanguage;
}
