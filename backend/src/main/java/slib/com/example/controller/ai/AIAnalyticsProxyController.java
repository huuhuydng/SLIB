package slib.com.example.controller.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Proxy controller để forward request từ mobile đến AI service
 * Mobile gọi: /slib/ai/analytics/...
 * Backend forward đến: AI_SERVICE_URL/api/ai/analytics/...
 */
@RestController
@RequestMapping("/slib/ai")
@RequiredArgsConstructor
public class AIAnalyticsProxyController {

    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Value("${slib.internal.api-key:}")
    private String internalApiKey;

    /**
     * Proxy: /slib/ai/analytics/density-prediction
     */
    @GetMapping("/analytics/density-prediction")
    public ResponseEntity<?> getDensityPrediction(
            @RequestParam(required = false) String zone_id,
            @RequestParam(required = false) Integer days) {
        StringBuilder url = new StringBuilder(aiServiceUrl + "/api/ai/analytics/density-prediction");
        boolean hasParam = false;
        if (zone_id != null) {
            url.append("?zone_id=").append(zone_id);
            hasParam = true;
        }
        if (days != null) {
            url.append(hasParam ? "&" : "?").append("days=").append(days);
        }
        ResponseEntity<Map> response = restTemplate.exchange(url.toString(), HttpMethod.GET, buildInternalRequest(), Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    /**
     * Proxy: /slib/ai/analytics/usage-statistics
     */
    @GetMapping("/analytics/usage-statistics")
    public ResponseEntity<?> getUsageStatistics(@RequestParam(defaultValue = "week") String period) {
        String url = aiServiceUrl + "/api/ai/analytics/usage-statistics?period=" + period;
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, buildInternalRequest(), Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    /**
     * Proxy: /slib/ai/analytics/realtime-capacity
     */
    @GetMapping("/analytics/realtime-capacity")
    public ResponseEntity<?> getRealtimeCapacity() {
        String url = aiServiceUrl + "/api/ai/analytics/realtime-capacity";
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, buildInternalRequest(), Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    /**
     * Proxy: /slib/ai/analytics/behavior-summary
     */
    @GetMapping("/analytics/behavior-summary")
    public ResponseEntity<?> getBehaviorSummary(@RequestParam(required = false) Integer days) {
        StringBuilder url = new StringBuilder(aiServiceUrl + "/api/ai/analytics/behavior-summary");
        if (days != null) {
            url.append("?days=").append(days);
        }
        ResponseEntity<Map> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                buildInternalRequest(),
                Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    /**
     * Proxy: /slib/ai/analytics/behavior-issues
     */
    @GetMapping("/analytics/behavior-issues")
    public ResponseEntity<?> getBehaviorIssues() {
        String url = aiServiceUrl + "/api/ai/analytics/behavior-issues";
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                buildInternalRequest(),
                Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    /**
     * Proxy: /slib/ai/analytics/seat-recommendation
     */
    @GetMapping("/analytics/seat-recommendation")
    public ResponseEntity<?> getSeatRecommendation(
            @RequestParam String user_id,
            @RequestParam(required = false) String zone_preference,
            @RequestParam(required = false) String time_slot,
            @RequestParam(required = false) String date) {

        StringBuilder url = new StringBuilder(aiServiceUrl + "/api/ai/analytics/seat-recommendation?user_id=" + user_id);
        if (zone_preference != null) url.append("&zone_preference=").append(zone_preference);
        if (time_slot != null) url.append("&time_slot=").append(time_slot);
        if (date != null) url.append("&date=").append(date);

        ResponseEntity<Map> response = restTemplate.exchange(
                url.toString(),
                HttpMethod.GET,
                buildInternalRequest(),
                Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    /**
     * Proxy: /slib/ai/analytics/student-behavior
     */
    @PostMapping("/analytics/student-behavior")
    public ResponseEntity<?> getStudentBehavior(@RequestBody Map<String, Object> body) {
        String url = aiServiceUrl + "/api/ai/analytics/student-behavior";
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                buildInternalRequest(body),
                Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    private HttpEntity<?> buildInternalRequest() {
        return buildInternalRequest(null);
    }

    private HttpEntity<?> buildInternalRequest(Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (internalApiKey != null && !internalApiKey.isBlank()) {
            headers.set("X-Internal-Api-Key", internalApiKey);
        }
        return new HttpEntity<>(body, headers);
    }
}
