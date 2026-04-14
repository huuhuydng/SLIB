package slib.com.example.dto.zone_config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutValidationResponse {
    private boolean valid;
    private boolean publishable;
    @Builder.Default
    private List<LayoutConflictResponse> conflicts = new ArrayList<>();
}
