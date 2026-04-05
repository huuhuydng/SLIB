package slib.com.example.controller.system;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * System Info Controller
 * Provides real-time system metrics (CPU, memory, disk, uptime, etc.)
 */
@RestController
@RequestMapping("/slib/system")
@RequiredArgsConstructor
public class SystemInfoController {

    private final DataSource dataSource;
    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url:http://127.0.0.1:8001}")
    private String aiServiceUrl;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // --- CPU ---
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        // Estimate CPU usage as percentage (load / cores * 100, capped at 100)
        double cpuSystemUsage = systemLoad >= 0 ? Math.min((systemLoad / availableProcessors) * 100, 100) : -1;
        double cpuProcessUsage = -1;
        long systemMemoryUsedMb = -1;
        long systemMemoryTotalMb = -1;
        double systemMemoryUsage = -1;

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            double processCpuLoad = sunBean.getProcessCpuLoad();
            double systemCpuLoad = sunBean.getCpuLoad();
            long totalPhysicalMemory = sunBean.getTotalMemorySize();
            long freePhysicalMemory = sunBean.getFreeMemorySize();

            if (processCpuLoad >= 0) {
                cpuProcessUsage = processCpuLoad * 100;
            }
            if (systemCpuLoad >= 0) {
                cpuSystemUsage = systemCpuLoad * 100;
            }
            if (totalPhysicalMemory > 0) {
                long usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory;
                systemMemoryUsedMb = usedPhysicalMemory / (1024 * 1024);
                systemMemoryTotalMb = totalPhysicalMemory / (1024 * 1024);
                systemMemoryUsage = (double) usedPhysicalMemory / totalPhysicalMemory * 100;
            }
        }

        info.put("cpu", roundOneDecimal(cpuSystemUsage));
        info.put("cpuProcess", roundOneDecimal(cpuProcessUsage));
        info.put("availableProcessors", availableProcessors);

        // --- Memory (JVM Heap) ---
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
        info.put("memory", roundOneDecimal(systemMemoryUsage >= 0 ? systemMemoryUsage : memoryUsage));
        info.put("jvmMemory", roundOneDecimal(memoryUsage));
        info.put("memoryUsedMB", heapUsed / (1024 * 1024));
        info.put("memoryMaxMB", heapMax / (1024 * 1024));
        info.put("systemMemoryUsedMB", systemMemoryUsedMb);
        info.put("systemMemoryTotalMB", systemMemoryTotalMb);

        // --- Disk ---
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        double diskUsage = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
        info.put("disk", roundOneDecimal(diskUsage));
        info.put("diskUsedGB", usedSpace / (1024L * 1024 * 1024));
        info.put("diskTotalGB", totalSpace / (1024L * 1024 * 1024));

        // --- Uptime ---
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeBean.getUptime();
        long uptimeSeconds = uptimeMs / 1000;
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        info.put("uptimeMs", uptimeMs);
        info.put("uptime", days + " ngày " + hours + " giờ " + minutes + " phút");

        // --- System Info ---
        info.put("osName", osBean.getName());
        info.put("osVersion", osBean.getVersion());
        info.put("osArch", osBean.getArch());
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("serverPort", System.getProperty("server.port", "8080"));
        info.put("checkedAt", LocalDateTime.now());
        info.put("services", buildServiceStatuses());
        info.put("overallStatus", resolveOverallStatus((Map<String, Object>) info.get("services")));

        return ResponseEntity.ok(info);
    }

    private Map<String, Object> buildServiceStatuses() {
        Map<String, Object> services = new HashMap<>();
        services.put("backend", Map.of(
                "status", "UP",
                "label", "Backend API",
                "detail", "Dịch vụ Spring Boot đang phản hồi"
        ));
        services.put("database", checkDatabaseStatus());
        services.put("ai", checkAiStatus());
        return services;
    }

    private Map<String, Object> checkDatabaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            return Map.of(
                    "status", valid ? "UP" : "DOWN",
                    "label", "PostgreSQL",
                    "detail", valid ? "Kết nối cơ sở dữ liệu ổn định" : "Không xác minh được kết nối cơ sở dữ liệu"
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "DOWN",
                    "label", "PostgreSQL",
                    "detail", "Không kết nối được cơ sở dữ liệu: " + e.getMessage()
            );
        }
    }

    private Map<String, Object> checkAiStatus() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(aiServiceUrl + "/health", Map.class);
            String rawStatus = response != null ? String.valueOf(response.getOrDefault("status", "unknown")) : "unknown";
            boolean up = "healthy".equalsIgnoreCase(rawStatus) || "up".equalsIgnoreCase(rawStatus);
            return Map.of(
                    "status", up ? "UP" : "DEGRADED",
                    "label", "AI Service",
                    "detail", up ? "Dịch vụ AI đang sẵn sàng" : "Dịch vụ AI phản hồi bất thường",
                    "rawStatus", rawStatus
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "DOWN",
                    "label", "AI Service",
                    "detail", "Không kết nối được dịch vụ AI"
            );
        }
    }

    private String resolveOverallStatus(Map<String, Object> services) {
        boolean hasDown = services.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(service -> String.valueOf(service.get("status")))
                .anyMatch("DOWN"::equalsIgnoreCase);
        if (hasDown) {
            return "CRITICAL";
        }

        boolean hasDegraded = services.values().stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(service -> String.valueOf(service.get("status")))
                .anyMatch("DEGRADED"::equalsIgnoreCase);
        return hasDegraded ? "DEGRADED" : "UP";
    }

    private double roundOneDecimal(double value) {
        if (value < 0) {
            return value;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
