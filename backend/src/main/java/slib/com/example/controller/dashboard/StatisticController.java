package slib.com.example.controller.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.dashboard.StatisticDTO;
import slib.com.example.service.dashboard.StatisticService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/slib/statistics")
@RequiredArgsConstructor
@Slf4j
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStatistics(
            @RequestParam(defaultValue = "week") String range) {
        try {
            byte[] excelContent = statisticService.exportStatisticsToExcel(range);
            String filename = "BaoCao_ThongKe_ThuVien_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy")) +
                    ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        } catch (Exception e) {
            log.error("Error exporting statistics report", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
