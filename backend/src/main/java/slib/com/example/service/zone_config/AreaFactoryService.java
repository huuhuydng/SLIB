package slib.com.example.service.zone_config;

import slib.com.example.repository.zone_config.AreaFactoryRepository;
import slib.com.example.repository.zone_config.AreaRepository;
import slib.com.example.dto.zone_config.AreaFactoryResponse;
import slib.com.example.entity.zone_config.AreaEntity;
import slib.com.example.entity.zone_config.AreaFactoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AreaFactoryService {

    private final AreaFactoryRepository areaFactoryRepository;
    private final AreaRepository areaRepository;

    /* ================= GET ================= */

    public List<AreaFactoryResponse> getAllAreaFactories() {
        return areaFactoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AreaFactoryResponse> getFactoriesByAreaId(Long areaId) {
        return areaFactoryRepository.findByArea_AreaId(areaId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AreaFactoryResponse getAreaFactoryById(Long factoryId) {
        return areaFactoryRepository.findById(factoryId)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Area Factory không tồn tại với ID: " + factoryId));
    }

    /* ================= CREATE ================= */

    public AreaFactoryResponse createAreaFactory(Long areaId, AreaFactoryEntity entity) {
        AreaEntity area = areaRepository.findById(areaId)
                .orElseThrow(() -> new RuntimeException("Area không tồn tại với ID: " + areaId));
        ensureAreaAllowsFactoryMutation(area, "thêm vật cản");

        entity.setArea(area);

        return toResponse(areaFactoryRepository.save(entity));
    }

    /* ================= UPDATE ================= */

    public AreaFactoryResponse updateAreaFactory(Long factoryId, AreaFactoryEntity req) {
        AreaFactoryEntity factory = areaFactoryRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Area Factory không tồn tại với ID: " + factoryId));
        ensureFactoryUpdateAllowed(factory, req);

        if (req.getFactoryName() != null)
            factory.setFactoryName(req.getFactoryName());
        if (req.getPositionX() != null)
            factory.setPositionX(req.getPositionX());
        if (req.getPositionY() != null)
            factory.setPositionY(req.getPositionY());
        if (req.getWidth() != null)
            factory.setWidth(req.getWidth());
        if (req.getHeight() != null)
            factory.setHeight(req.getHeight());
        if (req.getIsLocked() != null)
            factory.setIsLocked(req.getIsLocked());

        return toResponse(areaFactoryRepository.save(factory));
    }

    /* ================= DRAG ================= */

    public AreaFactoryResponse dragAreaFactory(Long factoryId, Integer x, Integer y) {
        AreaFactoryEntity factory = areaFactoryRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Area Factory không tồn tại với ID: " + factoryId));
        ensureFactoryAndAreaUnlocked(factory, "di chuyển vật cản");

        factory.setPositionX(x);
        factory.setPositionY(y);

        return toResponse(areaFactoryRepository.save(factory));
    }

    /* ================= RESIZE ================= */

    public AreaFactoryResponse resizeAreaFactory(Long factoryId, Integer width, Integer height) {
        AreaFactoryEntity factory = areaFactoryRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Area Factory không tồn tại với ID: " + factoryId));
        ensureFactoryAndAreaUnlocked(factory, "thay đổi kích thước vật cản");

        if (width != null && width > 0)
            factory.setWidth(width);
        if (height != null && height > 0)
            factory.setHeight(height);

        return toResponse(areaFactoryRepository.save(factory));
    }

    /* ================= DELETE ================= */

    public void deleteAreaFactory(Long factoryId) {
        AreaFactoryEntity factory = areaFactoryRepository.findById(factoryId)
                .orElseThrow(() -> new RuntimeException("Area Factory không tồn tại với ID: " + factoryId));
        ensureFactoryAndAreaUnlocked(factory, "xóa vật cản");
        areaFactoryRepository.deleteById(factoryId);
    }

    // MAP ENTITY -> DTO
    private AreaFactoryResponse toResponse(AreaFactoryEntity entity) {
        AreaFactoryResponse res = new AreaFactoryResponse();
        res.setFactoryId(entity.getFactoryId());
        res.setAreaId(entity.getArea() != null ? entity.getArea().getAreaId() : null);
        res.setFactoryName(entity.getFactoryName());
        res.setPositionX(entity.getPositionX());
        res.setPositionY(entity.getPositionY());
        res.setWidth(entity.getWidth());
        res.setHeight(entity.getHeight());
        res.setIsLocked(entity.getIsLocked());
        return res;
    }

    private void ensureAreaAllowsFactoryMutation(AreaEntity area, String action) {
        if (Boolean.TRUE.equals(area.getLocked())) {
            throw new RuntimeException("Không thể " + action + " khi phòng thư viện đang bị khóa");
        }
    }

    private void ensureFactoryAndAreaUnlocked(AreaFactoryEntity factory, String action) {
        ensureAreaAllowsFactoryMutation(factory.getArea(), action);
        if (Boolean.TRUE.equals(factory.getIsLocked())) {
            throw new RuntimeException("Không thể " + action + " khi vật cản đang bị khóa");
        }
    }

    private void ensureFactoryUpdateAllowed(AreaFactoryEntity factory, AreaFactoryEntity req) {
        ensureAreaAllowsFactoryMutation(factory.getArea(), "cập nhật vật cản");

        if (!Boolean.TRUE.equals(factory.getIsLocked())) {
            return;
        }

        boolean unlockOnly = Boolean.FALSE.equals(req.getIsLocked())
                && sameOrNull(req.getFactoryName(), factory.getFactoryName())
                && sameOrNull(req.getPositionX(), factory.getPositionX())
                && sameOrNull(req.getPositionY(), factory.getPositionY())
                && sameOrNull(req.getWidth(), factory.getWidth())
                && sameOrNull(req.getHeight(), factory.getHeight());

        boolean noEffectiveChange = (req.getIsLocked() == null || Boolean.TRUE.equals(req.getIsLocked()))
                && sameOrNull(req.getFactoryName(), factory.getFactoryName())
                && sameOrNull(req.getPositionX(), factory.getPositionX())
                && sameOrNull(req.getPositionY(), factory.getPositionY())
                && sameOrNull(req.getWidth(), factory.getWidth())
                && sameOrNull(req.getHeight(), factory.getHeight());

        if (!unlockOnly && !noEffectiveChange) {
            throw new RuntimeException("Không thể cập nhật vật cản đang bị khóa");
        }
    }

    private <T> boolean sameOrNull(T requestedValue, T currentValue) {
        return requestedValue == null || requestedValue.equals(currentValue);
    }

}
