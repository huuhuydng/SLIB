package slib.com.example.service.zone_config;

import slib.com.example.dto.zone_config.AreaResponse;
import slib.com.example.entity.zone_config.AreaEntity;
import slib.com.example.repository.zone_config.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;

    // =========================
    // GET ALL
    // =========================
    public List<AreaResponse> getAllAreas() {
        return areaRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // =========================
    // GET BY ID
    // =========================
    public AreaResponse getAreaById(Long id) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found with id: " + id));
        return toResponse(area);
    }

    // =========================
    // CREATE (FULL – từ FE)
    // =========================
    public AreaResponse createArea(AreaResponse req) {

        AreaEntity area = AreaEntity.builder()
                .areaName(req.getAreaName())
                .width(req.getWidth())
                .height(req.getHeight())
                .positionX(req.getPositionX())
                .positionY(req.getPositionY())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .locked(req.getLocked() != null ? req.getLocked() : false)
                .build();

        return toResponse(areaRepository.save(area));
    }

    // =========================
    // CREATE QUICK (default)
    // =========================
    public AreaResponse createArea(String areaName) {

        AreaEntity area = AreaEntity.builder()
                .areaName(areaName)
                .width(800)
                .height(600)
                .positionX(0)
                .positionY(0)
                .isActive(true)
                .locked(false)
                .build();

        return toResponse(areaRepository.save(area));
    }

    // =========================
    // UPDATE FULL (info + position + size + state)
    // =========================
    public AreaResponse updateAreaFull(Long id, AreaResponse req) {

        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found"));

        area.setAreaName(req.getAreaName());
        area.setWidth(req.getWidth());
        area.setHeight(req.getHeight());
        area.setPositionX(req.getPositionX());
        area.setPositionY(req.getPositionY());

        if (req.getIsActive() != null) {
            area.setIsActive(req.getIsActive());
        }
        if (req.getLocked() != null) {
            area.setLocked(req.getLocked());
        }

        return toResponse(areaRepository.save(area));
    }

    // =========================
    // UPDATE POSITION ONLY (drag & drop)
    // =========================
    public AreaResponse updateAreaPosition(Long id, AreaResponse req) {

        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found"));

        if (Boolean.TRUE.equals(area.getLocked())) {
            throw new RuntimeException("Area is locked");
        }

        area.setPositionX(req.getPositionX());
        area.setPositionY(req.getPositionY());

        return toResponse(areaRepository.save(area));
    }

    // =========================
    // UPDATE DIMENSIONS ONLY (resize)
    // =========================
    public AreaResponse updateAreaDimensions(Long id, AreaResponse req) {

        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found"));

        if (Boolean.TRUE.equals(area.getLocked())) {
            throw new RuntimeException("Area is locked");
        }

        area.setWidth(req.getWidth());
        area.setHeight(req.getHeight());

        return toResponse(areaRepository.save(area));
    }

    // =========================
    // UPDATE POSITION + DIMENSIONS
    // =========================
    public AreaResponse updateAreaPositionAndDimensions(Long id, AreaResponse req) {

        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found"));

        if (Boolean.TRUE.equals(area.getLocked())) {
            throw new RuntimeException("Area is locked");
        }

        area.setPositionX(req.getPositionX());
        area.setPositionY(req.getPositionY());
        area.setWidth(req.getWidth());
        area.setHeight(req.getHeight());

        return toResponse(areaRepository.save(area));
    }

    // =========================
    // DELETE
    // =========================
    public void deleteArea(Long id) {
        areaRepository.deleteById(id);
    }

    // =========================
    // UPDATE LOCK STATUS ONLY
    // =========================
    public AreaResponse updateAreaLocked(Long id, AreaResponse req) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found"));
        
        area.setLocked(req.getLocked());
        return toResponse(areaRepository.save(area));
    }

    // =========================
    // UPDATE ACTIVE STATUS ONLY
    // =========================
    public AreaResponse updateAreaIsActive(Long id, AreaResponse req) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Area not found"));
        
        area.setIsActive(req.getIsActive());
        return toResponse(areaRepository.save(area));
    }

    // =========================
    // MAP ENTITY -> DTO
    // =========================
    private AreaResponse toResponse(AreaEntity area) {

        AreaResponse res = new AreaResponse();
        res.setAreaId(area.getAreaId());
        res.setAreaName(area.getAreaName());
        res.setWidth(area.getWidth());
        res.setHeight(area.getHeight());
        res.setPositionX(area.getPositionX());
        res.setPositionY(area.getPositionY());
        res.setIsActive(area.getIsActive());
        res.setLocked(area.getLocked());

        return res;
    }
}
