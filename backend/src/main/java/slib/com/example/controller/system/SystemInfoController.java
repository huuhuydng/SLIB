package slib.com.example.controller.system;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.io.File;
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

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        // --- CPU ---
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();
        // Estimate CPU usage as percentage (load / cores * 100, capped at 100)
        double cpuUsage = systemLoad >= 0 ? Math.min((systemLoad / availableProcessors) * 100, 100) : -1;

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
            // More accurate CPU usage from com.sun.management
            double processCpuLoad = sunBean.getProcessCpuLoad();
            if (processCpuLoad >= 0) {
                cpuUsage = processCpuLoad * 100;
            }
        }

        info.put("cpu", Math.round(cpuUsage * 10.0) / 10.0);
        info.put("availableProcessors", availableProcessors);

        // --- Memory (JVM Heap) ---
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = heapMax > 0 ? (double) heapUsed / heapMax * 100 : 0;
        info.put("memory", Math.round(memoryUsage * 10.0) / 10.0);
        info.put("memoryUsedMB", heapUsed / (1024 * 1024));
        info.put("memoryMaxMB", heapMax / (1024 * 1024));

        // --- Disk ---
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        double diskUsage = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
        info.put("disk", Math.round(diskUsage * 10.0) / 10.0);
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

        return ResponseEntity.ok(info);
    }
}
