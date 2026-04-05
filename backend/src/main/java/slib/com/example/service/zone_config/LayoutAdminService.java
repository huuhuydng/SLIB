package slib.com.example.service.zone_config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.zone_config.*;
import slib.com.example.entity.zone_config.*;
import slib.com.example.entity.users.User;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.feedback.SeatStatusReportRepository;
import slib.com.example.repository.feedback.SeatViolationReportRepository;
import slib.com.example.repository.zone_config.*;
import slib.com.example.service.users.UserService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LayoutAdminService {

    private final AreaRepository areaRepository;
    private final ZoneRepository zoneRepository;
    private final SeatRepository seatRepository;
    private final AreaFactoryRepository areaFactoryRepository;
    private final AmenityRepository amenityRepository;
    private final ReservationRepository reservationRepository;
    private final SeatStatusReportRepository seatStatusReportRepository;
    private final SeatViolationReportRepository seatViolationReportRepository;
    private final LayoutDraftRepository layoutDraftRepository;
    private final LayoutHistoryRepository layoutHistoryRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public LayoutDraftResponse getDraftOrPublishedSnapshot() {
        Optional<LayoutDraftEntity> draft = layoutDraftRepository.findAll().stream().findFirst();
        if (draft.isPresent()) {
            LayoutDraftEntity entity = draft.get();
            return LayoutDraftResponse.builder()
                    .hasDraft(true)
                    .basedOnPublishedVersion(entity.getBasedOnPublishedVersion())
                    .updatedByName(entity.getUpdatedByName())
                    .updatedAt(entity.getUpdatedAt())
                    .snapshot(readSnapshot(entity.getSnapshotJson()))
                    .build();
        }

        return LayoutDraftResponse.builder()
                .hasDraft(false)
                .basedOnPublishedVersion(layoutHistoryRepository.findLatestPublishedVersion().orElse(0L))
                .snapshot(buildCurrentSnapshot())
                .build();
    }

    @Transactional(readOnly = true)
    public List<LayoutHistoryResponse> getHistory(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return layoutHistoryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(history -> LayoutHistoryResponse.builder()
                        .historyId(history.getHistoryId())
                        .actionType(history.getActionType())
                        .summary(history.getSummary())
                        .publishedVersion(history.getPublishedVersion())
                        .createdByName(history.getCreatedByName())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public LayoutValidationResponse validate(LayoutSnapshotRequest snapshot) {
        return validateSnapshot(normalizeSnapshot(snapshot));
    }

    @Transactional
    public LayoutDraftResponse saveDraft(LayoutSnapshotRequest snapshot, Authentication authentication) {
        LayoutSnapshotRequest normalized = normalizeSnapshot(snapshot);
        LayoutValidationResponse validation = validateSnapshot(normalized);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Sơ đồ còn xung đột, vui lòng xử lý trước khi lưu nháp");
        }

        ActorInfo actor = resolveActor(authentication);
        LayoutDraftEntity entity = layoutDraftRepository.findAll().stream().findFirst()
                .orElse(LayoutDraftEntity.builder().build());

        entity.setSnapshotJson(writeSnapshot(normalized));
        entity.setBasedOnPublishedVersion(layoutHistoryRepository.findLatestPublishedVersion().orElse(0L));
        entity.setUpdatedByUserId(actor.userId());
        entity.setUpdatedByName(actor.displayName());
        entity.setUpdatedAt(LocalDateTime.now());
        layoutDraftRepository.save(entity);

        recordHistory("SAVE_DRAFT", buildSummary("Đã lưu nháp sơ đồ", normalized), normalized, null, actor);

        return LayoutDraftResponse.builder()
                .hasDraft(true)
                .basedOnPublishedVersion(entity.getBasedOnPublishedVersion())
                .updatedByName(entity.getUpdatedByName())
                .updatedAt(entity.getUpdatedAt())
                .snapshot(normalized)
                .build();
    }

    @Transactional
    public LayoutPublishResponse publish(LayoutSnapshotRequest snapshot, Authentication authentication) {
        LayoutSnapshotRequest normalized = normalizeSnapshot(snapshot);
        LayoutValidationResponse validation = validateSnapshot(normalized);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Sơ đồ còn xung đột, vui lòng xử lý trước khi xuất bản");
        }

        ActorInfo actor = resolveActor(authentication);
        applySnapshot(normalized);
        LayoutSnapshotRequest publishedSnapshot = buildCurrentSnapshot();
        long publishedVersion = layoutHistoryRepository.findLatestPublishedVersion().orElse(0L) + 1;

        layoutDraftRepository.deleteAll();
        recordHistory("PUBLISH", buildSummary("Đã xuất bản sơ đồ", publishedSnapshot), publishedSnapshot, publishedVersion, actor);

        return LayoutPublishResponse.builder()
                .publishedVersion(publishedVersion)
                .publishedByName(actor.displayName())
                .snapshot(publishedSnapshot)
                .build();
    }

    private LayoutValidationResponse validateSnapshot(LayoutSnapshotRequest snapshot) {
        List<LayoutConflictResponse> conflicts = new ArrayList<>();

        Map<Long, AreaResponse> areasById = snapshot.getAreas().stream()
                .collect(Collectors.toMap(
                        area -> area.getAreaId() == null ? Long.MIN_VALUE + snapshot.getAreas().indexOf(area) : area.getAreaId(),
                        Function.identity(),
                        (left, right) -> right,
                        LinkedHashMap::new));

        Map<Integer, ZoneResponse> zonesById = snapshot.getZones().stream()
                .collect(Collectors.toMap(
                        zone -> zone.getZoneId() == null ? Integer.MIN_VALUE + snapshot.getZones().indexOf(zone) : zone.getZoneId(),
                        Function.identity(),
                        (left, right) -> right,
                        LinkedHashMap::new));

        validateAreaOverlaps(snapshot.getAreas(), conflicts);
        validateZones(snapshot.getZones(), areasById, conflicts);
        validateFactories(snapshot.getFactories(), areasById, snapshot.getZones(), conflicts);
        validateSeats(snapshot.getSeats(), zonesById, conflicts);

        return LayoutValidationResponse.builder()
                .valid(conflicts.isEmpty())
                .conflicts(conflicts)
                .build();
    }

    private void validateAreaOverlaps(List<AreaResponse> areas, List<LayoutConflictResponse> conflicts) {
        for (int i = 0; i < areas.size(); i++) {
            AreaResponse current = areas.get(i);
            if (current.getWidth() == null || current.getHeight() == null) {
                conflicts.add(conflict("AREA_INVALID_SIZE", "error", "area", areaKey(current),
                        "Kích thước phòng thư viện không hợp lệ",
                        "Phòng thư viện phải có chiều rộng và chiều cao lớn hơn 0."));
                continue;
            }

            for (int j = i + 1; j < areas.size(); j++) {
                AreaResponse other = areas.get(j);
                if (isOverlap(
                        current.getPositionX(), current.getPositionY(), current.getWidth(), current.getHeight(),
                        other.getPositionX(), other.getPositionY(), other.getWidth(), other.getHeight())) {
                    conflicts.add(conflict("AREA_OVERLAP", "error", "area", areaKey(current),
                            "Phòng thư viện bị chồng lấn",
                            "Phòng \"" + safeName(current.getAreaName(), "Chưa đặt tên") + "\" đang chồng lên phòng \""
                                    + safeName(other.getAreaName(), "Chưa đặt tên") + "\"."));
                }
            }
        }
    }

    private void validateZones(List<ZoneResponse> zones, Map<Long, AreaResponse> areasById,
                               List<LayoutConflictResponse> conflicts) {
        Map<Long, List<ZoneResponse>> zonesByArea = zones.stream()
                .collect(Collectors.groupingBy(zone -> zone.getAreaId() == null ? Long.MIN_VALUE : zone.getAreaId()));

        for (ZoneResponse zone : zones) {
            AreaResponse area = areasById.get(zone.getAreaId());
            if (area == null) {
                conflicts.add(conflict("ZONE_MISSING_AREA", "error", "zone", zoneKey(zone),
                        "Khu vực ghế chưa thuộc phòng thư viện",
                        "Khu vực \"" + safeName(zone.getZoneName(), "Chưa đặt tên") + "\" chưa liên kết với phòng thư viện hợp lệ."));
                continue;
            }

            if (!isInside(zone.getPositionX(), zone.getPositionY(), zone.getWidth(), zone.getHeight(),
                    area.getWidth(), area.getHeight())) {
                conflicts.add(conflict("ZONE_OUT_OF_AREA", "error", "zone", zoneKey(zone),
                        "Khu vực ghế vượt khỏi phòng thư viện",
                        "Khu vực \"" + safeName(zone.getZoneName(), "Chưa đặt tên") + "\" đang nằm ngoài khung của phòng \""
                                + safeName(area.getAreaName(), "Chưa đặt tên") + "\"."));
            }
        }

        for (Map.Entry<Long, List<ZoneResponse>> entry : zonesByArea.entrySet()) {
            List<ZoneResponse> group = entry.getValue();
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    ZoneResponse current = group.get(i);
                    ZoneResponse other = group.get(j);
                    if (isOverlap(
                            current.getPositionX(), current.getPositionY(), current.getWidth(), current.getHeight(),
                            other.getPositionX(), other.getPositionY(), other.getWidth(), other.getHeight())) {
                        conflicts.add(conflict("ZONE_OVERLAP", "error", "zone", zoneKey(current),
                                "Khu vực ghế bị chồng lấn",
                                "Khu vực \"" + safeName(current.getZoneName(), "Chưa đặt tên") + "\" đang chồng lên khu vực \""
                                        + safeName(other.getZoneName(), "Chưa đặt tên") + "\"."));
                    }
                }
            }
        }
    }

    private void validateFactories(List<AreaFactoryResponse> factories, Map<Long, AreaResponse> areasById,
                                   List<ZoneResponse> zones, List<LayoutConflictResponse> conflicts) {
        Map<Long, List<AreaFactoryResponse>> factoriesByArea = factories.stream()
                .collect(Collectors.groupingBy(factory -> factory.getAreaId() == null ? Long.MIN_VALUE : factory.getAreaId()));
        Map<Long, List<ZoneResponse>> zonesByArea = zones.stream()
                .collect(Collectors.groupingBy(zone -> zone.getAreaId() == null ? Long.MIN_VALUE : zone.getAreaId()));

        for (AreaFactoryResponse factory : factories) {
            AreaResponse area = areasById.get(factory.getAreaId());
            if (area == null) {
                conflicts.add(conflict("FACTORY_MISSING_AREA", "error", "factory", factoryKey(factory),
                        "Vật cản chưa thuộc phòng thư viện",
                        "Vật cản \"" + safeName(factory.getFactoryName(), "Chưa đặt tên") + "\" chưa liên kết với phòng thư viện hợp lệ."));
                continue;
            }

            if (!isInside(factory.getPositionX(), factory.getPositionY(), factory.getWidth(), factory.getHeight(),
                    area.getWidth(), area.getHeight())) {
                conflicts.add(conflict("FACTORY_OUT_OF_AREA", "error", "factory", factoryKey(factory),
                        "Vật cản vượt khỏi phòng thư viện",
                        "Vật cản \"" + safeName(factory.getFactoryName(), "Chưa đặt tên") + "\" đang nằm ngoài khung phòng \""
                                + safeName(area.getAreaName(), "Chưa đặt tên") + "\"."));
            }
        }

        for (Map.Entry<Long, List<AreaFactoryResponse>> entry : factoriesByArea.entrySet()) {
            List<AreaFactoryResponse> group = entry.getValue();
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    AreaFactoryResponse current = group.get(i);
                    AreaFactoryResponse other = group.get(j);
                    if (isOverlap(
                            current.getPositionX(), current.getPositionY(), current.getWidth(), current.getHeight(),
                            other.getPositionX(), other.getPositionY(), other.getWidth(), other.getHeight())) {
                        conflicts.add(conflict("FACTORY_OVERLAP", "warning", "factory", factoryKey(current),
                                "Vật cản bị chồng lấn",
                                "Vật cản \"" + safeName(current.getFactoryName(), "Chưa đặt tên") + "\" đang chồng lên vật cản khác trong cùng phòng."));
                    }
                }
            }

            List<ZoneResponse> zonesInArea = zonesByArea.getOrDefault(entry.getKey(), List.of());
            for (AreaFactoryResponse factory : group) {
                for (ZoneResponse zone : zonesInArea) {
                    if (isOverlap(
                            factory.getPositionX(), factory.getPositionY(), factory.getWidth(), factory.getHeight(),
                            zone.getPositionX(), zone.getPositionY(), zone.getWidth(), zone.getHeight())) {
                        conflicts.add(conflict("FACTORY_ZONE_OVERLAP", "warning", "factory", factoryKey(factory),
                                "Vật cản đang đè lên khu vực ghế",
                                "Vật cản \"" + safeName(factory.getFactoryName(), "Chưa đặt tên") + "\" đang chồng lên khu vực \""
                                        + safeName(zone.getZoneName(), "Chưa đặt tên") + "\"."));
                    }
                }
            }
        }
    }

    private void validateSeats(List<SeatResponse> seats, Map<Integer, ZoneResponse> zonesById,
                               List<LayoutConflictResponse> conflicts) {
        Map<Integer, Set<String>> seatCodesByZone = new HashMap<>();
        Map<Integer, Set<String>> seatPositionsByZone = new HashMap<>();

        for (SeatResponse seat : seats) {
            ZoneResponse zone = zonesById.get(seat.getZoneId());
            if (zone == null) {
                conflicts.add(conflict("SEAT_MISSING_ZONE", "error", "seat", seatKey(seat),
                        "Ghế chưa thuộc khu vực ghế",
                        "Ghế \"" + safeName(seat.getSeatCode(), "Chưa đặt mã ghế") + "\" chưa liên kết với khu vực hợp lệ."));
                continue;
            }

            String seatCode = safeName(seat.getSeatCode(), "").trim().toUpperCase(Locale.ROOT);
            if (!seatCode.isEmpty()) {
                Set<String> codes = seatCodesByZone.computeIfAbsent(zone.getZoneId(), ignored -> new HashSet<>());
                if (!codes.add(seatCode)) {
                    conflicts.add(conflict("SEAT_DUPLICATE_CODE", "error", "seat", seatKey(seat),
                            "Mã ghế đang bị trùng",
                            "Mã ghế \"" + seatCode + "\" đang bị trùng trong khu vực \"" + safeName(zone.getZoneName(), "Chưa đặt tên") + "\"."));
                }
            }

            String rowColKey = (seat.getRowNumber() == null ? "null" : seat.getRowNumber()) + ":" +
                    (seat.getColumnNumber() == null ? "null" : seat.getColumnNumber());
            Set<String> positions = seatPositionsByZone.computeIfAbsent(zone.getZoneId(), ignored -> new HashSet<>());
            if (!positions.add(rowColKey)) {
                conflicts.add(conflict("SEAT_DUPLICATE_POSITION", "error", "seat", seatKey(seat),
                        "Vị trí ghế đang bị trùng",
                        "Ghế \"" + safeName(seat.getSeatCode(), "Chưa đặt mã ghế") + "\" bị trùng hàng/cột trong khu vực \""
                                + safeName(zone.getZoneName(), "Chưa đặt tên") + "\"."));
            }
        }
    }

    @Transactional(readOnly = true)
    public LayoutSnapshotRequest buildCurrentSnapshot() {
        List<AreaResponse> areas = areaRepository.findAll().stream()
                .sorted(Comparator.comparing(AreaEntity::getAreaId))
                .map(this::toAreaResponse)
                .toList();
        Map<Integer, List<AmenityResponse>> amenitiesByZone = amenityRepository.findAll().stream()
                .map(this::toAmenityResponse)
                .collect(Collectors.groupingBy(AmenityResponse::getZoneId));
        List<ZoneResponse> zones = zoneRepository.findAll().stream()
                .sorted(Comparator.comparing(ZoneEntity::getZoneId))
                .map(zone -> toZoneResponse(zone, amenitiesByZone.getOrDefault(zone.getZoneId(), List.of())))
                .toList();
        List<SeatResponse> seats = seatRepository.findAll().stream()
                .sorted(Comparator.comparing(SeatEntity::getSeatId))
                .map(this::toSeatResponse)
                .toList();
        List<AreaFactoryResponse> factories = areaFactoryRepository.findAll().stream()
                .sorted(Comparator.comparing(AreaFactoryEntity::getFactoryId))
                .map(this::toFactoryResponse)
                .toList();

        return new LayoutSnapshotRequest(areas, zones, seats, factories);
    }

    @Transactional
    protected void applySnapshot(LayoutSnapshotRequest snapshot) {
        Map<Long, AreaEntity> liveAreas = areaRepository.findAll().stream()
                .collect(Collectors.toMap(AreaEntity::getAreaId, Function.identity()));
        Map<Integer, ZoneEntity> liveZones = zoneRepository.findAll().stream()
                .collect(Collectors.toMap(ZoneEntity::getZoneId, Function.identity()));
        Map<Integer, SeatEntity> liveSeats = seatRepository.findAll().stream()
                .collect(Collectors.toMap(SeatEntity::getSeatId, Function.identity()));
        Map<Long, AreaFactoryEntity> liveFactories = areaFactoryRepository.findAll().stream()
                .collect(Collectors.toMap(AreaFactoryEntity::getFactoryId, Function.identity()));

        Map<Long, Long> areaIdMap = new HashMap<>();
        for (AreaResponse areaDto : snapshot.getAreas()) {
            Long requestedId = areaDto.getAreaId();
            AreaEntity area = requestedId != null && requestedId > 0 ? liveAreas.get(requestedId) : null;
            if (area == null) {
                area = AreaEntity.builder().build();
            }
            area.setAreaName(areaDto.getAreaName());
            area.setWidth(areaDto.getWidth());
            area.setHeight(areaDto.getHeight());
            area.setPositionX(defaultZero(areaDto.getPositionX()));
            area.setPositionY(defaultZero(areaDto.getPositionY()));
            area.setIsActive(areaDto.getIsActive() != null ? areaDto.getIsActive() : true);
            area.setLocked(areaDto.getLocked() != null ? areaDto.getLocked() : false);
            AreaEntity saved = areaRepository.save(area);
            areaIdMap.put(requestedId != null ? requestedId : -saved.getAreaId(), saved.getAreaId());
            liveAreas.remove(saved.getAreaId());
        }

        Map<Integer, Integer> zoneIdMap = new HashMap<>();
        for (ZoneResponse zoneDto : snapshot.getZones()) {
            Integer requestedId = zoneDto.getZoneId();
            ZoneEntity zone = requestedId != null && requestedId > 0 ? liveZones.get(requestedId) : null;
            if (zone == null) {
                zone = ZoneEntity.builder().build();
            }
            Long resolvedAreaId = resolveAreaId(zoneDto.getAreaId(), areaIdMap);
            AreaEntity area = areaRepository.findById(resolvedAreaId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng thư viện khi xuất bản sơ đồ"));

            zone.setArea(area);
            zone.setZoneName(zoneDto.getZoneName());
            zone.setZoneDes(zoneDto.getZoneDes());
            zone.setPositionX(defaultZero(zoneDto.getPositionX()));
            zone.setPositionY(defaultZero(zoneDto.getPositionY()));
            zone.setWidth(zoneDto.getWidth());
            zone.setHeight(zoneDto.getHeight());
            zone.setIsLocked(zoneDto.getIsLocked() != null ? zoneDto.getIsLocked() : false);
            ZoneEntity saved = zoneRepository.save(zone);
            zoneIdMap.put(requestedId != null ? requestedId : -saved.getZoneId(), saved.getZoneId());
            liveZones.remove(saved.getZoneId());
        }

        amenityRepository.deleteAll();
        for (ZoneResponse zoneDto : snapshot.getZones()) {
            Integer resolvedZoneId = resolveZoneId(zoneDto.getZoneId(), zoneIdMap);
            ZoneEntity zone = zoneRepository.findById(resolvedZoneId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực ghế cho tiện ích khi xuất bản sơ đồ"));

            List<AmenityResponse> amenities = zoneDto.getAmenities() != null ? zoneDto.getAmenities() : List.of();
            for (AmenityResponse amenityDto : amenities) {
                if (amenityDto.getAmenityName() == null || amenityDto.getAmenityName().isBlank()) {
                    continue;
                }
                amenityRepository.save(AmenityEntity.builder()
                        .zone(zone)
                        .amenityName(amenityDto.getAmenityName().trim())
                        .build());
            }
        }

        for (AreaFactoryResponse factoryDto : snapshot.getFactories()) {
            Long requestedId = factoryDto.getFactoryId();
            AreaFactoryEntity factory = requestedId != null && requestedId > 0 ? liveFactories.get(requestedId) : null;
            if (factory == null) {
                factory = AreaFactoryEntity.builder().build();
            }

            Long resolvedAreaId = resolveAreaId(factoryDto.getAreaId(), areaIdMap);
            AreaEntity area = areaRepository.findById(resolvedAreaId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng thư viện cho vật cản"));

            factory.setArea(area);
            factory.setFactoryName(factoryDto.getFactoryName());
            factory.setPositionX(defaultZero(factoryDto.getPositionX()));
            factory.setPositionY(defaultZero(factoryDto.getPositionY()));
            factory.setWidth(factoryDto.getWidth());
            factory.setHeight(factoryDto.getHeight());
            factory.setIsLocked(factoryDto.getIsLocked() != null ? factoryDto.getIsLocked() : false);
            AreaFactoryEntity saved = areaFactoryRepository.save(factory);
            liveFactories.remove(saved.getFactoryId());
        }

        for (SeatResponse seatDto : snapshot.getSeats()) {
            Integer requestedId = seatDto.getSeatId();
            SeatEntity seat = requestedId != null && requestedId > 0 ? liveSeats.get(requestedId) : null;
            if (seat == null) {
                seat = SeatEntity.builder().build();
            }

            Integer resolvedZoneId = resolveZoneId(seatDto.getZoneId(), zoneIdMap);
            ZoneEntity zone = zoneRepository.findById(resolvedZoneId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khu vực ghế cho ghế khi xuất bản sơ đồ"));

            seat.setZone(zone);
            seat.setSeatCode(seatDto.getSeatCode());
            seat.setRowNumber(seatDto.getRowNumber());
            seat.setColumnNumber(seatDto.getColumnNumber());
            seat.setIsActive(seatDto.getIsActive() != null ? seatDto.getIsActive() : true);
            seat.setNfcTagUid(seatDto.getNfcTagUid());
            seatRepository.save(seat);
            if (requestedId != null && requestedId > 0) {
                liveSeats.remove(requestedId);
            }
        }

        for (SeatEntity seat : liveSeats.values()) {
            reservationRepository.deleteBySeat_SeatId(seat.getSeatId());
            seatStatusReportRepository.deleteBySeat_SeatId(seat.getSeatId());
            seatViolationReportRepository.deleteBySeat_SeatId(seat.getSeatId());
            seatRepository.deleteById(seat.getSeatId());
        }

        for (AreaFactoryEntity factory : liveFactories.values()) {
            areaFactoryRepository.deleteById(factory.getFactoryId());
        }

        for (ZoneEntity zone : liveZones.values()) {
            List<SeatEntity> seatsInZone = seatRepository.findByZone_ZoneId(zone.getZoneId());
            for (SeatEntity seat : seatsInZone) {
                reservationRepository.deleteBySeat_SeatId(seat.getSeatId());
                seatStatusReportRepository.deleteBySeat_SeatId(seat.getSeatId());
                seatViolationReportRepository.deleteBySeat_SeatId(seat.getSeatId());
            }
            amenityRepository.deleteByZone_ZoneId(zone.getZoneId());
            seatRepository.deleteByZone_ZoneId(zone.getZoneId());
            zoneRepository.deleteById(zone.getZoneId());
        }

        for (AreaEntity area : liveAreas.values()) {
            List<ZoneEntity> zonesInArea = zoneRepository.findByArea_AreaId(area.getAreaId());
            for (ZoneEntity zone : zonesInArea) {
                List<SeatEntity> seatsInZone = seatRepository.findByZone_ZoneId(zone.getZoneId());
                for (SeatEntity seat : seatsInZone) {
                    reservationRepository.deleteBySeat_SeatId(seat.getSeatId());
                    seatStatusReportRepository.deleteBySeat_SeatId(seat.getSeatId());
                    seatViolationReportRepository.deleteBySeat_SeatId(seat.getSeatId());
                }
                amenityRepository.deleteByZone_ZoneId(zone.getZoneId());
                seatRepository.deleteByZone_ZoneId(zone.getZoneId());
            }
            areaFactoryRepository.deleteByArea_AreaId(area.getAreaId());
            zoneRepository.deleteAll(zonesInArea);
            areaRepository.deleteById(area.getAreaId());
        }
    }

    private void recordHistory(String actionType, String summary, LayoutSnapshotRequest snapshot,
                               Long publishedVersion, ActorInfo actor) {
        layoutHistoryRepository.save(LayoutHistoryEntity.builder()
                .actionType(actionType)
                .summary(summary)
                .snapshotJson(writeSnapshot(snapshot))
                .publishedVersion(publishedVersion)
                .createdByUserId(actor.userId())
                .createdByName(actor.displayName())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private LayoutSnapshotRequest normalizeSnapshot(LayoutSnapshotRequest snapshot) {
        if (snapshot == null) {
            return new LayoutSnapshotRequest(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        return new LayoutSnapshotRequest(
                snapshot.getAreas() != null ? snapshot.getAreas() : new ArrayList<>(),
                snapshot.getZones() != null ? snapshot.getZones() : new ArrayList<>(),
                snapshot.getSeats() != null ? snapshot.getSeats() : new ArrayList<>(),
                snapshot.getFactories() != null ? snapshot.getFactories() : new ArrayList<>());
    }

    private LayoutSnapshotRequest readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, LayoutSnapshotRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Không thể đọc dữ liệu nháp sơ đồ", e);
        }
    }

    private String writeSnapshot(LayoutSnapshotRequest snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Không thể lưu snapshot sơ đồ", e);
        }
    }

    private ActorInfo resolveActor(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return new ActorInfo(null, "Quản trị viên");
        }

        try {
            User user = userService.getUserByEmail(authentication.getName());
            return new ActorInfo(user.getId(), user.getFullName());
        } catch (Exception ex) {
            log.warn("Không thể xác định người thao tác sơ đồ từ authentication {}: {}", authentication.getName(),
                    ex.getMessage());
            return new ActorInfo(null, authentication.getName());
        }
    }

    private LayoutConflictResponse conflict(String code, String severity, String entityType, String entityKey,
                                            String title, String message) {
        return LayoutConflictResponse.builder()
                .code(code)
                .severity(severity)
                .entityType(entityType)
                .entityKey(entityKey)
                .title(title)
                .message(message)
                .build();
    }

    private boolean isOverlap(Integer x1, Integer y1, Integer w1, Integer h1,
                              Integer x2, Integer y2, Integer w2, Integer h2) {
        int left1 = defaultZero(x1);
        int top1 = defaultZero(y1);
        int right1 = left1 + Math.max(defaultZero(w1), 0);
        int bottom1 = top1 + Math.max(defaultZero(h1), 0);
        int left2 = defaultZero(x2);
        int top2 = defaultZero(y2);
        int right2 = left2 + Math.max(defaultZero(w2), 0);
        int bottom2 = top2 + Math.max(defaultZero(h2), 0);
        return left1 < right2 && right1 > left2 && top1 < bottom2 && bottom1 > top2;
    }

    private boolean isInside(Integer childX, Integer childY, Integer childWidth, Integer childHeight,
                             Integer parentWidth, Integer parentHeight) {
        int x = defaultZero(childX);
        int y = defaultZero(childY);
        int width = defaultZero(childWidth);
        int height = defaultZero(childHeight);
        int maxWidth = defaultZero(parentWidth);
        int maxHeight = defaultZero(parentHeight);
        return x >= 0 && y >= 0 && width > 0 && height > 0 && x + width <= maxWidth && y + height <= maxHeight;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildSummary(String prefix, LayoutSnapshotRequest snapshot) {
        return prefix + ": "
                + snapshot.getAreas().size() + " phòng, "
                + snapshot.getZones().size() + " khu vực, "
                + snapshot.getSeats().size() + " ghế, "
                + snapshot.getFactories().size() + " vật cản";
    }

    private Long resolveAreaId(Long requestedAreaId, Map<Long, Long> areaIdMap) {
        if (requestedAreaId == null) {
            throw new RuntimeException("Thiếu areaId khi xuất bản sơ đồ");
        }
        if (requestedAreaId > 0) {
            return requestedAreaId;
        }
        Long mapped = areaIdMap.get(requestedAreaId);
        if (mapped == null) {
            throw new RuntimeException("Không thể ánh xạ phòng thư viện mới khi xuất bản sơ đồ");
        }
        return mapped;
    }

    private Integer resolveZoneId(Integer requestedZoneId, Map<Integer, Integer> zoneIdMap) {
        if (requestedZoneId == null) {
            throw new RuntimeException("Thiếu zoneId khi xuất bản sơ đồ");
        }
        if (requestedZoneId > 0) {
            return requestedZoneId;
        }
        Integer mapped = zoneIdMap.get(requestedZoneId);
        if (mapped == null) {
            throw new RuntimeException("Không thể ánh xạ khu vực ghế mới khi xuất bản sơ đồ");
        }
        return mapped;
    }

    private AreaResponse toAreaResponse(AreaEntity area) {
        return new AreaResponse(
                area.getAreaId(),
                area.getAreaName(),
                area.getWidth(),
                area.getHeight(),
                area.getPositionX(),
                area.getPositionY(),
                area.getIsActive(),
                area.getLocked());
    }

    private ZoneResponse toZoneResponse(ZoneEntity zone, List<AmenityResponse> amenities) {
        ZoneResponse response = new ZoneResponse();
        response.setZoneId(zone.getZoneId());
        response.setZoneName(zone.getZoneName());
        response.setZoneDes(zone.getZoneDes());
        response.setPositionX(zone.getPositionX());
        response.setPositionY(zone.getPositionY());
        response.setWidth(zone.getWidth());
        response.setHeight(zone.getHeight());
        response.setAreaId(zone.getArea().getAreaId());
        response.setIsLocked(zone.getIsLocked());
        response.setAmenities(amenities != null ? new ArrayList<>(amenities) : new ArrayList<>());
        return response;
    }

    private AmenityResponse toAmenityResponse(AmenityEntity amenity) {
        return new AmenityResponse(
                amenity.getAmenityId(),
                amenity.getZone().getZoneId(),
                amenity.getAmenityName()
        );
    }

    private SeatResponse toSeatResponse(SeatEntity seat) {
        SeatResponse response = new SeatResponse();
        response.setSeatId(seat.getSeatId());
        response.setZoneId(seat.getZone().getZoneId());
        response.setSeatCode(seat.getSeatCode());
        response.setRowNumber(seat.getRowNumber());
        response.setColumnNumber(seat.getColumnNumber());
        response.setIsActive(seat.getIsActive());
        response.setNfcTagUid(seat.getNfcTagUid());
        response.setSeatStatus(seat.getSeatStatus());
        return response;
    }

    private AreaFactoryResponse toFactoryResponse(AreaFactoryEntity factory) {
        return new AreaFactoryResponse(
                factory.getFactoryId(),
                factory.getFactoryName(),
                factory.getArea().getAreaId(),
                factory.getPositionX(),
                factory.getPositionY(),
                factory.getWidth(),
                factory.getHeight(),
                factory.getIsLocked());
    }

    private String areaKey(AreaResponse area) {
        return "area:" + (area.getAreaId() != null ? area.getAreaId() : safeName(area.getAreaName(), "new"));
    }

    private String zoneKey(ZoneResponse zone) {
        return "zone:" + (zone.getZoneId() != null ? zone.getZoneId() : safeName(zone.getZoneName(), "new"));
    }

    private String factoryKey(AreaFactoryResponse factory) {
        return "factory:" + (factory.getFactoryId() != null ? factory.getFactoryId() : safeName(factory.getFactoryName(), "new"));
    }

    private String seatKey(SeatResponse seat) {
        return "seat:" + (seat.getSeatId() != null ? seat.getSeatId() : safeName(seat.getSeatCode(), "new"));
    }

    private String safeName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ActorInfo(UUID userId, String displayName) {
    }
}
