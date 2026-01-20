    package slib.com.example.service;

    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;

import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.SeatRepository;
    import slib.com.example.repository.ZoneRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class SeatService {

        // ===== CONSTANTS (BẮT BUỘC) =====
        private static final int ROW_HEIGHT = 40;
        private static final int DEFAULT_SEAT_WIDTH = 80;

        private final SeatRepository seatRepository;
        private final ZoneRepository zoneRepository;

        // ================= GET =================

        public List<SeatResponse> getSeatsByZoneId(Integer zoneId) {
            return seatRepository.findByZone_ZoneId(zoneId)
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        public List<SeatResponse> getAllSeats() {
            return seatRepository.findAll()
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        public SeatResponse getSeatById(Integer id) {
            SeatEntity seat = seatRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        return toResponse(seat);
    }

    // ================= GET BY TIME RANGE =================
    public List<SeatResponse> getSeatsByTimeRange(String startTimeStr, String endTimeStr, Integer zoneId) {
        // Parse ISO 8601 time strings (e.g., "2026-01-20T13:00:00")
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
        
        List<SeatEntity> seats;
        if (zoneId != null) {
            seats = seatRepository.findByZone_ZoneId(zoneId);
        } else {
            seats = seatRepository.findAll();
        }
        
        return seats.stream()
                .map(seat -> {
                    SeatResponse response = toResponse(seat);
                    
                    // Tính toán status động dựa trên reservations trong time range
                    boolean isBookedInTimeRange = seat.getReservation().stream()
                            .anyMatch(r -> {
                                String status = r.getStatus();
                                boolean isActiveStatus = "BOOKED".equalsIgnoreCase(status) 
                                                      || "PROCESSING".equalsIgnoreCase(status)
                                                      || "CONFIRMED".equalsIgnoreCase(status);
                                
                                if (!isActiveStatus) {
                                    return false;
                                }
                                
                                // Kiểm tra overlap: reservation có giao với time range không
                                LocalDateTime resStart = r.getStartTime();
                                LocalDateTime resEnd = r.getEndTime();
                                
                                return resStart.isBefore(endTime) && resEnd.isAfter(startTime);
                            });
                    
                    // Nếu seat có status UNAVAILABLE trong DB, ưu tiên status này
                    if (seat.getSeatStatus() == SeatStatus.UNAVAILABLE) {
                        response.setSeatStatus(SeatStatus.UNAVAILABLE);
                    } else if (isBookedInTimeRange) {
                        response.setSeatStatus(SeatStatus.BOOKED);
                    } else {
                        response.setSeatStatus(SeatStatus.AVAILABLE);
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());
    }

    // ================= CREATE =================
    // FE chỉ gửi: zoneId, rowNumber
    public SeatResponse createSeat(SeatResponse req) {
            ZoneEntity zone = zoneRepository.findById(req.getZoneId())
                    .orElseThrow(() -> new RuntimeException("Zone not found"));

            int rowNumber = req.getRowNumber();

            // tìm column tiếp theo trong cùng row
            Integer maxColumn = seatRepository
                    .findMaxColumnByZoneIdAndRow(zone.getZoneId(), rowNumber);

            int columnNumber = (maxColumn == null) ? 1 : maxColumn + 1;

            int seatWidth = req.getWidth() != null ? req.getWidth() : DEFAULT_SEAT_WIDTH;

            int positionX = (columnNumber - 1) * seatWidth;
            int positionY = (rowNumber - 1) * ROW_HEIGHT;

            String seatCode = generateSeatCode(rowNumber, columnNumber);

            SeatEntity seat = SeatEntity.builder()
                    .zone(zone)
                    .rowNumber(rowNumber)
                    .columnNumber(columnNumber)
                    .seatCode(seatCode)
                    .seatStatus(req.getSeatStatus() != null ? req.getSeatStatus() : SeatStatus.AVAILABLE)
                    .width(seatWidth)
                    .height(ROW_HEIGHT)
                    .positionX(positionX)
                    .positionY(positionY)
                    .build();

            return toResponse(seatRepository.save(seat));
        }

        // ================= UPDATE FULL =================
        // update theo row + column → tự tính lại seatCode & position
        public SeatResponse updateSeatFull(Integer id, SeatResponse req) {
            SeatEntity seat = seatRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));

            int rowNumber = req.getRowNumber();
            int columnNumber = req.getColumnNumber();
            int seatWidth = req.getWidth() != null ? req.getWidth() : seat.getWidth();

            seat.setRowNumber(rowNumber);
            seat.setColumnNumber(columnNumber);
            seat.setSeatCode(generateSeatCode(rowNumber, columnNumber));
            seat.setSeatStatus(req.getSeatStatus() != null ? req.getSeatStatus() : seat.getSeatStatus());
            seat.setWidth(seatWidth);
            seat.setHeight(ROW_HEIGHT);
            seat.setPositionX((columnNumber - 1) * seatWidth);
            seat.setPositionY((rowNumber - 1) * ROW_HEIGHT);

            return toResponse(seatRepository.save(seat));
        }

        // ================= UPDATE POSITION (KÉO THẢ) =================
        public SeatResponse updateSeatPosition(Integer id, SeatResponse req) {
            SeatEntity seat = seatRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));

            seat.setPositionX(req.getPositionX());
            seat.setPositionY(req.getPositionY());

            return toResponse(seatRepository.save(seat));
        }

        // ================= UPDATE DIMENSIONS =================
        public SeatResponse updateSeatDimensions(Integer id, SeatResponse req) {
            SeatEntity seat = seatRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));

            seat.setWidth(req.getWidth());
            seat.setHeight(req.getHeight());

            return toResponse(seatRepository.save(seat));
        }

        // ================= UPDATE POSITION + DIMENSIONS =================
        public SeatResponse updateSeatPositionAndDimensions(Integer id, SeatResponse req) {
            SeatEntity seat = seatRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));

            seat.setPositionX(req.getPositionX());
            seat.setPositionY(req.getPositionY());
            seat.setWidth(req.getWidth());
            seat.setHeight(req.getHeight());

            return toResponse(seatRepository.save(seat));
        }

        // ================= DELETE =================
        public void deleteSeat(Integer id) {
            if (!seatRepository.existsById(id)) {
                throw new RuntimeException("Seat not found");
            }
            seatRepository.deleteById(id);
        }


        // ================= UPDATE SEAT (FULL) =================
        public SeatResponse updateSeat(Integer id, SeatResponse req) {
            SeatEntity seat = seatRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Seat not found"));

            if (req.getSeatCode() != null) {
                seat.setSeatCode(req.getSeatCode());
            }
            if (req.getSeatStatus() != null) {
                seat.setSeatStatus(req.getSeatStatus());
            }
            if (req.getPositionX() != null) {
                seat.setPositionX(req.getPositionX());
            }
            if (req.getPositionY() != null) {
                seat.setPositionY(req.getPositionY());
            }
            if (req.getWidth() != null) {
                seat.setWidth(req.getWidth());
            }
            if (req.getHeight() != null) {
                seat.setHeight(req.getHeight());
            }

            return toResponse(seatRepository.save(seat));
        }

        // ================= RESTRICTION OPERATIONS =================
        /**
         * Hạn chế ghế (set status to UNAVAILABLE)
         */
        public SeatResponse restrictSeat(String seatCode) {
            SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));
            
            seat.setSeatStatus(SeatStatus.UNAVAILABLE);
            return toResponse(seatRepository.save(seat));
        }

        /**
         * Bỏ hạn chế ghế (set status back to AVAILABLE)
         */
        public void unrestrictSeat(String seatCode) {
            SeatEntity seat = seatRepository.findBySeatCode(seatCode)
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatCode));
            
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        // ================= UTIL =================
        private String generateSeatCode(int row, int column) {
            char rowChar = (char) ('A' + row - 1);
            return rowChar + String.valueOf(column);
        }

        // ================= MAP ENTITY -> DTO =================
        private SeatResponse toResponse(SeatEntity seat) {
            SeatResponse res = new SeatResponse();
            res.setSeatId(seat.getSeatId());
            res.setZoneId(seat.getZone().getZoneId());
            res.setSeatCode(seat.getSeatCode());
            res.setSeatStatus(seat.getSeatStatus());
            res.setPositionX(seat.getPositionX());
            res.setPositionY(seat.getPositionY());
            res.setRowNumber(seat.getRowNumber());
            res.setColumnNumber(seat.getColumnNumber());
            res.setWidth(seat.getWidth());
            res.setHeight(seat.getHeight());
            return res;
        }
    }
