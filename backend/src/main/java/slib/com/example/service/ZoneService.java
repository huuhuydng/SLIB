package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import slib.com.example.dto.zone_config.ZoneResponse;
import slib.com.example.entity.zone_config.AreaEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.AreaRepository;
import slib.com.example.repository.ZoneRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final AreaRepository areaRepository;

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

    // DELETE
    public void deleteZone(Integer id) {
        if (!zoneRepository.existsById(id)) {
            throw new RuntimeException("Zone not found");
        }
        zoneRepository.deleteById(id);
    }

    // MAP ENTITY -> DTO
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
}
