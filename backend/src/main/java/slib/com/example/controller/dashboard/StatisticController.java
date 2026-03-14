package slib.com.example.controller.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.dashboard.StatisticDTO;
import slib.com.example.service.dashboard.StatisticService;

@RestController
@RequestMapping("/slib/statistics")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    /**
     * Lấy toàn bộ thống kê theo khoảng thời gian
     * 
     * @param range week | month | year
     */
    @GetMapping
    public ResponseEntity<StatisticDTO> getStatistics(
            @RequestParam(defaultValue = "week") String range) {
        return ResponseEntity.ok(statisticService.getStatistics(range));
    }
}
