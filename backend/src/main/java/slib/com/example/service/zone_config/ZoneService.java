package slib.com.example.service.zone_config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import slib.com.example.dto.zone_config.ZoneOccupancyDTO;
import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.entity.zone_config.AreaEntity;
import slib.com.example.entity.zone_config.SeatEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.zone_config.AreaRepository;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.zone_config.SeatRepository;
import slib.com.example.repository.zone_config.AmenityRepository;
import slib.com.example.repository.zone_config.ZoneRepository;
import slib.com.example.repository.feedback.SeatStatusReportRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final AreaRepository areaRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final AmenityRepository amenityRepository;
    private final SeatStatusReportRepository seatStatusReportRepository;
    private final SeatViolationReportRepository seatViolationReportRepository;
    // GET zones theo areaId
    public List<ZoneResponse> getZonesByAreaId(Long areaId) {
        return zoneRepository.findByArea_AreaId(areaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET ALL
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET BY ID
    public ZoneResponse getZoneById(Integer id) {
        ZoneEntity zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));
        return toResponse(zone);
    }

    // CREATE
    public ZoneResponse createZone(ZoneResponse req) {

        AreaEntity area = areaRepository.findById(req.getAreaId())
                .orElseThrow(() -> new RuntimeException("Area not found"));

        ZoneEntity zone = ZoneEntity.builder()
                .zoneName(req.getZoneName())
                .zoneDes(req.getZoneDes())
                .area(area)

                // LẤY TỪ FRONTEND
                .positionX(req.getPositionX())
                .positionY(req.getPositionY())
                .width(req.getWidth())
                .height(req.getHeight())
                .isLocked(req.getIsLocked() != null ? req.getIsLocked() : false)
                .build();

        return toResponse(zoneRepository.save(zone));
    }

    // UPDATE (thông tin)
    public ZoneResponse updateZone(Integer id, ZoneResponse req) {
        ZoneEntity zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        if (req.getZoneName() != null)
            zone.setZoneName(req.getZoneName());
        if (req.getZoneDes() != null)
            zone.setZoneDes(req.getZoneDes());
        if (req.getIsLocked() != null)
            zone.setIsLocked(req.getIsLocked());

        return toResponse(zoneRepository.save(zone));
    }

    // UPDATE POSITION (KÉO THẢ) - chỉ cập nhật vị trí
    public ZoneResponse updateZonePosition(Integer id, ZoneResponse req) {
        ZoneEntity zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        zone.setPositionX(req.getPositionX());
        zone.setPositionY(req.getPositionY());
        if (req.getIsLocked() != null)
            zone.setIsLocked(req.getIsLocked());

        return toResponse(zoneRepository.save(zone));
    }

    // UPDATE DIMENSIONS (RESIZE) - chỉ cập nhật kích thước
    public ZoneResponse updateZoneDimensions(Integer id, ZoneResponse req) {
        ZoneEntity zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        zone.setWidth(req.getWidth());
        zone.setHeight(req.getHeight());
        if (req.getIsLocked() != null)
            zone.setIsLocked(req.getIsLocked());

        return toResponse(zoneRepository.save(zone));
    }

    // UPDATE POSITION AND DIMENSIONS - cập nhật cả vị trí và kích thước
    public ZoneResponse updateZonePositionAndDimensions(Integer id, ZoneResponse req) {
        ZoneEntity zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        zone.setPositionX(req.getPositionX());
        zone.setPositionY(req.getPositionY());
        zone.setWidth(req.getWidth());
        zone.setHeight(req.getHeight());

        return toResponse(zoneRepository.save(zone));
    }

    // UPDATE VỊ TRÍ - INFOR
    public ZoneResponse updateZoneFull(Integer id, ZoneResponse req) {
        ZoneEntity zone = zoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        // info
        if (req.getZoneName() != null)
            zone.setZoneName(req.getZoneName());
        if (req.getZoneDes() != null)
            zone.setZoneDes(req.getZoneDes());
        if (req.getIsLocked() != null)
            zone.setIsLocked(req.getIsLocked());

        // position + size
        if (req.getPositionX() != null)
            zone.setPositionX(req.getPositionX());
        if (req.getPositionY() != null)
            zone.setPositionY(req.getPositionY());
        if (req.getWidth() != null)
            zone.setWidth(req.getWidth());
        if (req.getHeight() != null)
            zone.setHeight(req.getHeight());

        return toResponse(zoneRepository.save(zone));
    }

    // DELETE - xóa reservations, seats, rồi zone
    public void deleteZone(Integer id) {
        if (!zoneRepository.existsById(id)) {
            throw new RuntimeException("Zone not found");
        }

        // 1. Get all seats in this zone
        List<SeatEntity> seatsInZone = seatRepository.findByZone_ZoneId(id);

        // 2. Delete all reservations for each seat first
        for (SeatEntity seat : seatsInZone) {
            reservationRepository.deleteBySeat_SeatId(seat.getSeatId());
            seatStatusReportRepository.deleteBySeat_SeatId(seat.getSeatId());
            seatViolationReportRepository.deleteBySeat_SeatId(seat.getSeatId());
        }

        // 3. Delete all amenities in this zone
        amenityRepository.deleteByZone_ZoneId(id);

        // 4. Delete all seats in this zone
        seatRepository.deleteByZone_ZoneId(id);

        // 5. Now delete the zone
        zoneRepository.deleteById(id);
    }

    private ZoneResponse toResponse(ZoneEntity zone) {
        ZoneResponse res = new ZoneResponse();
        res.setZoneId(zone.getZoneId());
        res.setZoneName(zone.getZoneName());
        res.setZoneDes(zone.getZoneDes());

        // mapping cho kéo thả
        res.setPositionX(zone.getPositionX());
        res.setPositionY(zone.getPositionY());
        res.setWidth(zone.getWidth());
        res.setHeight(zone.getHeight());
        res.setAreaId(zone.getArea().getAreaId());
        res.setIsLocked(zone.getIsLocked());

        return res;
    }

    /**
     * Get zone occupancy information for all zones in an area
     * Used by mobile app to display zone density colors
     */
    public List<ZoneOccupancyDTO> getZoneOccupancy(Long areaId) {
        List<ZoneEntity> zones = zoneRepository.findByArea_AreaId(areaId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        return zones.stream().map(zone -> {
            // Get all active seats in zone
            List<SeatEntity> activeSeats = seatRepository.findByZone_ZoneIdAndIsActiveTrue(zone.getZoneId());
            long totalSeats = activeSeats.size();

            // Count occupied seats (those with active reservations right now)
            long occupiedSeats = activeSeats.stream()
                    .filter(seat -> {
                        var overlapping = reservationRepository.findOverlappingReservations(
                                seat.getSeatId(), now, now.plusMinutes(1));
                        return !overlapping.isEmpty();
                    })
                    .count();

            // Calculate occupancy rate (0.0 to 1.0)
            double occupancyRate = totalSeats > 0 ? (double) occupiedSeats / totalSeats : 0.0;

            return ZoneOccupancyDTO.builder()
                    .zoneId(zone.getZoneId())
                    .zoneName(zone.getZoneName())
                    .positionX(zone.getPositionX())
                    .positionY(zone.getPositionY())
                    .width(zone.getWidth())
                    .height(zone.getHeight())
                    .totalSeats(totalSeats)
                    .occupiedSeats(occupiedSeats)
                    .occupancyRate(occupancyRate)
                    .build();
        }).collect(Collectors.toList());
    }
}
