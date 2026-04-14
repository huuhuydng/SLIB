package slib.com.example.service.zone_config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.users.UserService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LayoutAdminService {
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final String SCHEDULE_STATUS_PENDING = "PENDING";
    public static final String SCHEDULE_STATUS_EXECUTING = "EXECUTING";
    public static final String SCHEDULE_STATUS_EXECUTED = "EXECUTED";
    public static final String SCHEDULE_STATUS_CANCELLED = "CANCELLED";
    public static final String SCHEDULE_STATUS_FAILED = "FAILED";

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
    private final LayoutScheduleRepository layoutScheduleRepository;
    private final UserService userService;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public LayoutDraftResponse getDraftOrPublishedSnapshot(Authentication authentication) {
        ActorInfo actor = resolveRequiredActor(authentication);
        Optional<LayoutDraftEntity> draft = layoutDraftRepository.findFirstByUpdatedByUserId(actor.userId());
        if (draft.isPresent()) {
            LayoutDraftEntity entity = draft.get();
            try {
                return LayoutDraftResponse.builder()
                        .hasDraft(true)
                        .basedOnPublishedVersion(entity.getBasedOnPublishedVersion())
                        .updatedByName(entity.getUpdatedByName())
                        .updatedAt(entity.getUpdatedAt())
                        .scheduledPublish(getActiveSchedule())
                        .snapshot(readSnapshot(entity.getSnapshotJson()))
                        .build();
            } catch (RuntimeException ex) {
                log.error("Không thể đọc nháp sơ đồ hiện tại, chuyển sang snapshot xuất bản gần nhất: {}", ex.getMessage());
            }
        }

        return LayoutDraftResponse.builder()
                .hasDraft(false)
                .basedOnPublishedVersion(layoutHistoryRepository.findLatestPublishedVersion().orElse(0L))
                .scheduledPublish(getActiveSchedule())
                .snapshot(buildCurrentSnapshot())
                .build();
    }

    @Transactional(readOnly = true)
    public LayoutScheduleResponse getActiveSchedule() {
        return layoutScheduleRepository.findFirstByStatusOrderByScheduledForAsc(SCHEDULE_STATUS_PENDING)
                .map(this::toScheduleResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<LayoutHistoryResponse> getHistory(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return layoutHistoryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(history -> LayoutHistoryResponse.builder()
                        .historyId(history.getHistoryId())
                        .actionType(history.getActionType())
                        .summary(resolveStoredText(history.getSummary(), "layout_history.summary"))
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

        ActorInfo actor = resolveRequiredActor(authentication);
        LayoutDraftEntity entity = layoutDraftRepository.findFirstByUpdatedByUserId(actor.userId())
                .orElse(LayoutDraftEntity.builder().build());
        long latestPublishedVersion = layoutHistoryRepository.findLatestPublishedVersion().orElse(0L);

        entity.setSnapshotJson(writeSnapshot(normalized));
        entity.setBasedOnPublishedVersion(resolveBasedOnPublishedVersion(normalized, entity.getBasedOnPublishedVersion(),
                latestPublishedVersion));
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
                .scheduledPublish(getActiveSchedule())
                .snapshot(normalized)
                .build();
    }

    @Transactional
    public LayoutDraftResponse discardDraft(Authentication authentication) {
        ActorInfo actor = resolveRequiredActor(authentication);
        if (layoutDraftRepository.findFirstByUpdatedByUserId(actor.userId()).isPresent()) {
            LayoutSnapshotRequest publishedSnapshot = buildCurrentSnapshot();
            layoutDraftRepository.deleteByUpdatedByUserId(actor.userId());
            recordHistory("DISCARD_DRAFT", "Đã bỏ nháp sơ đồ và quay về bản xuất bản",
                    publishedSnapshot, layoutHistoryRepository.findLatestPublishedVersion().orElse(0L), actor);
        }

        return LayoutDraftResponse.builder()
                .hasDraft(false)
                .basedOnPublishedVersion(layoutHistoryRepository.findLatestPublishedVersion().orElse(0L))
                .scheduledPublish(getActiveSchedule())
                .snapshot(buildCurrentSnapshot())
                .build();
    }

    @Transactional
    public LayoutScheduleResponse schedulePublish(LayoutScheduleRequest request, Authentication authentication) {
        if (request == null || request.getSnapshot() == null) {
            throw new IllegalArgumentException("Thiếu snapshot sơ đồ để lên lịch xuất bản");
        }
        if (request.getScheduledFor() == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời điểm áp dụng sơ đồ");
        }

        LayoutSnapshotRequest normalized = normalizeSnapshot(request.getSnapshot());
        LayoutValidationResponse validation = validateSnapshot(normalized);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Sơ đồ còn xung đột hình học, chưa thể lên lịch xuất bản");
        }

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        if (!request.getScheduledFor().isAfter(now)) {
            throw new IllegalArgumentException("Thời điểm áp dụng phải lớn hơn thời gian hiện tại");
        }

        ActorInfo actor = resolveRequiredActor(authentication);
        long latestPublishedVersion = layoutHistoryRepository.findLatestPublishedVersion().orElse(0L);
        long basedOnPublishedVersion = resolveBasedOnPublishedVersion(normalized, null, latestPublishedVersion);
        if (basedOnPublishedVersion != latestPublishedVersion) {
            throw new IllegalStateException("Sơ đồ đã được người khác xuất bản phiên bản mới. Vui lòng tải lại trước khi lên lịch.");
        }

        LayoutScheduleEntity entity = layoutScheduleRepository.findFirstByStatusOrderByScheduledForAsc(SCHEDULE_STATUS_PENDING)
                .orElse(LayoutScheduleEntity.builder()
                        .createdAt(now)
                        .build());
        boolean isReschedule = entity.getScheduleId() != null;

        entity.setSnapshotJson(writeSnapshot(normalized));
        entity.setBasedOnPublishedVersion(basedOnPublishedVersion);
        entity.setScheduledFor(request.getScheduledFor());
        entity.setStatus(SCHEDULE_STATUS_PENDING);
        entity.setLastError(null);
        entity.setRequestedByUserId(actor.userId());
        entity.setRequestedByName(actor.displayName());
        entity.setUpdatedAt(now);
        entity.setCancelledAt(null);
        entity.setExecutedAt(null);

        LayoutScheduleEntity saved = layoutScheduleRepository.save(entity);
        recordHistory(
                isReschedule ? "RESCHEDULE_PUBLISH" : "SCHEDULE_PUBLISH",
                buildScheduleSummary(isReschedule ? "Đã cập nhật lịch xuất bản sơ đồ" : "Đã lên lịch xuất bản sơ đồ",
                        normalized,
                        saved.getScheduledFor()),
                normalized,
                null,
                actor
        );
        publishScheduleChangedEvent();
        return toScheduleResponse(saved);
    }

    @Transactional
    public LayoutScheduleResponse cancelScheduledPublish(Long scheduleId, Authentication authentication) {
        LayoutScheduleEntity entity = layoutScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch xuất bản sơ đồ"));
        if (!SCHEDULE_STATUS_PENDING.equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalArgumentException("Lịch này không còn ở trạng thái chờ để hủy");
        }

        ActorInfo actor = resolveRequiredActor(authentication);
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        entity.setStatus(SCHEDULE_STATUS_CANCELLED);
        entity.setCancelledAt(now);
        entity.setUpdatedAt(now);
        entity.setLastError(null);

        LayoutScheduleEntity saved = layoutScheduleRepository.save(entity);
        recordHistory(
                "CANCEL_SCHEDULE_PUBLISH",
                "Đã hủy lịch xuất bản sơ đồ dự kiến vào " + formatScheduleTime(saved.getScheduledFor()),
                readSnapshot(saved.getSnapshotJson()),
                null,
                actor
        );
        publishScheduleChangedEvent();
        return toScheduleResponse(saved);
    }

    @Transactional
    public LayoutPublishResponse publish(LayoutSnapshotRequest snapshot, Authentication authentication) {
        LayoutSnapshotRequest normalized = normalizeSnapshot(snapshot);
        LayoutValidationResponse validation = validateSnapshot(normalized);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Sơ đồ còn xung đột, vui lòng xử lý trước khi xuất bản");
        }

        ActorInfo actor = resolveRequiredActor(authentication);
        return publishInternal(normalized, actor, "PUBLISH", "Đã xuất bản sơ đồ");
    }

    @Transactional
    public void executeScheduledPublish(Long scheduleId) {
        LayoutScheduleEntity schedule = layoutScheduleRepository.findById(scheduleId)
                .orElse(null);
        if (schedule == null || !SCHEDULE_STATUS_PENDING.equalsIgnoreCase(schedule.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        if (schedule.getScheduledFor() != null && schedule.getScheduledFor().isAfter(now)) {
            return;
        }

        schedule.setStatus(SCHEDULE_STATUS_EXECUTING);
        schedule.setUpdatedAt(now);
        layoutScheduleRepository.save(schedule);

        try {
            LayoutSnapshotRequest snapshot = readSnapshot(schedule.getSnapshotJson());
            LayoutValidationResponse validation = validateSnapshot(snapshot);
            if (!validation.isValid() || !validation.isPublishable()) {
                throw new IllegalStateException(resolveScheduleFailureReason(validation));
            }

            publishInternal(snapshot,
                    new ActorInfo(schedule.getRequestedByUserId(), schedule.getRequestedByName()),
                    "AUTO_PUBLISH",
                    "Đã tự động xuất bản sơ đồ theo lịch");

            schedule.setStatus(SCHEDULE_STATUS_EXECUTED);
            schedule.setExecutedAt(LocalDateTime.now(VIETNAM_ZONE));
            schedule.setUpdatedAt(schedule.getExecutedAt());
            schedule.setLastError(null);
            layoutScheduleRepository.save(schedule);
        } catch (Exception ex) {
            log.warn("Tự động xuất bản sơ đồ thất bại cho lịch {}: {}", scheduleId, ex.getMessage());
            schedule.setStatus(SCHEDULE_STATUS_FAILED);
            schedule.setLastError(ex.getMessage());
            schedule.setUpdatedAt(LocalDateTime.now(VIETNAM_ZONE));
            layoutScheduleRepository.save(schedule);
            recordHistory(
                    "AUTO_PUBLISH_FAILED",
                    "Lịch tự động xuất bản sơ đồ thất bại: " + ex.getMessage(),
                    readSnapshot(schedule.getSnapshotJson()),
                    null,
                    new ActorInfo(schedule.getRequestedByUserId(), schedule.getRequestedByName())
            );
        } finally {
            publishScheduleChangedEvent();
        }
    }

    private LayoutPublishResponse publishInternal(LayoutSnapshotRequest normalized,
                                                 ActorInfo actor,
                                                 String actionType,
                                                 String summaryPrefix) {
        long latestPublishedVersion = layoutHistoryRepository.findLatestPublishedVersion().orElse(0L);
        long basedOnPublishedVersion = resolveBasedOnPublishedVersion(normalized, null, latestPublishedVersion);
        if (basedOnPublishedVersion != latestPublishedVersion) {
            throw new IllegalStateException("Sơ đồ đã được người khác xuất bản phiên bản mới. Vui lòng tải lại trước khi xuất bản tiếp.");
        }

        LayoutSnapshotRequest currentSnapshot = buildCurrentSnapshot();
        LayoutChangeImpact impact = analyzeLayoutChangeImpact(currentSnapshot, normalized);
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        assertPublishAllowed(impact, now);
        List<slib.com.example.entity.booking.ReservationEntity> futureWarnReservations =
                findFutureReservations(impact.warnOnlySeatIds(), now);
        Set<UUID> futureWarnReservationIds = futureWarnReservations.stream()
                .map(slib.com.example.entity.booking.ReservationEntity::getReservationId)
                .collect(Collectors.toSet());
        List<slib.com.example.entity.booking.ReservationEntity> staleWarnings =
                reservationRepository.findFutureLayoutChangedReservations(now).stream()
                        .filter(reservation -> !futureWarnReservationIds.contains(reservation.getReservationId()))
                        .toList();

        applySnapshot(normalized);
        LayoutSnapshotRequest publishedSnapshot = buildCurrentSnapshot();
        long publishedVersion = latestPublishedVersion + 1;

        bookingService.clearLayoutChangeWarnings(staleWarnings);
        bookingService.markReservationsAffectedByLayoutChange(futureWarnReservations, now);

        layoutDraftRepository.deleteAllInBatch();
        cancelPendingSchedulesAfterImmediatePublish(actor, actionType, now);
        recordHistory(actionType, buildSummary(summaryPrefix, publishedSnapshot), publishedSnapshot, publishedVersion, actor);

        return LayoutPublishResponse.builder()
                .publishedVersion(publishedVersion)
                .publishedByName(actor.displayName())
                .snapshot(publishedSnapshot)
                .build();
    }

    private LayoutChangeImpact analyzeLayoutChangeImpact(LayoutSnapshotRequest currentSnapshot,
                                                         LayoutSnapshotRequest nextSnapshot) {
        Map<Long, AreaResponse> currentAreas = currentSnapshot.getAreas().stream()
                .filter(area -> area.getAreaId() != null)
                .collect(Collectors.toMap(AreaResponse::getAreaId, Function.identity()));
        Map<Long, AreaResponse> nextAreas = nextSnapshot.getAreas().stream()
                .filter(area -> area.getAreaId() != null)
                .collect(Collectors.toMap(AreaResponse::getAreaId, Function.identity()));
        Map<Integer, ZoneResponse> currentZones = currentSnapshot.getZones().stream()
                .filter(zone -> zone.getZoneId() != null)
                .collect(Collectors.toMap(ZoneResponse::getZoneId, Function.identity()));
        Map<Integer, ZoneResponse> nextZones = nextSnapshot.getZones().stream()
                .filter(zone -> zone.getZoneId() != null)
                .collect(Collectors.toMap(ZoneResponse::getZoneId, Function.identity()));
        Map<Integer, SeatResponse> nextSeats = nextSnapshot.getSeats().stream()
                .filter(seat -> seat.getSeatId() != null)
                .collect(Collectors.toMap(SeatResponse::getSeatId, Function.identity()));

        Set<Integer> blockedSeatIds = new LinkedHashSet<>();
        Set<Integer> destructiveSeatIds = new LinkedHashSet<>();
        Set<Integer> warnOnlySeatIds = new LinkedHashSet<>();

        for (SeatResponse currentSeat : currentSnapshot.getSeats()) {
            Integer seatId = currentSeat.getSeatId();
            if (seatId == null) {
                continue;
            }

            ZoneResponse currentZone = currentZones.get(currentSeat.getZoneId());
            AreaResponse currentArea = currentZone != null ? currentAreas.get(currentZone.getAreaId()) : null;
            SeatResponse nextSeat = nextSeats.get(seatId);

            if (nextSeat == null) {
                blockedSeatIds.add(seatId);
                destructiveSeatIds.add(seatId);
                continue;
            }

            ZoneResponse nextZone = nextZones.get(nextSeat.getZoneId());
            AreaResponse nextArea = nextZone != null ? nextAreas.get(nextZone.getAreaId()) : null;

            boolean destructive = nextZone == null
                    || nextArea == null
                    || !Boolean.TRUE.equals(nextSeat.getIsActive())
                    || !Boolean.TRUE.equals(nextArea.getIsActive());

            if (destructive) {
                blockedSeatIds.add(seatId);
                destructiveSeatIds.add(seatId);
                continue;
            }

            boolean seatChanged = hasSeatStructureChanged(currentSeat, nextSeat);
            boolean zoneChanged = currentZone != null && nextZone != null && hasZoneStructureChanged(currentZone, nextZone);
            boolean areaChanged = currentArea != null && nextArea != null && hasAreaStructureChanged(currentArea, nextArea);

            if (seatChanged || zoneChanged || areaChanged) {
                blockedSeatIds.add(seatId);
                warnOnlySeatIds.add(seatId);
            }
        }

        return new LayoutChangeImpact(blockedSeatIds, destructiveSeatIds, warnOnlySeatIds);
    }

    private void assertPublishAllowed(LayoutChangeImpact impact, LocalDateTime now) {
        List<slib.com.example.entity.booking.ReservationEntity> currentReservations =
                findCurrentReservations(impact.blockedSeatIds(), now);
        if (!currentReservations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không thể xuất bản vì có ghế đang được sử dụng hoặc giữ chỗ trong sơ đồ bị thay đổi: "
                            + summarizeSeats(currentReservations) + ".");
        }

        List<slib.com.example.entity.booking.ReservationEntity> futureDestructiveReservations =
                findFutureReservations(impact.destructiveSeatIds(), now);
        if (!futureDestructiveReservations.isEmpty()) {
            throw new IllegalArgumentException(
                    "Không thể xuất bản vì có lịch đặt sắp tới ở các ghế sẽ bị gỡ hoặc ngừng hoạt động: "
                            + summarizeSeats(futureDestructiveReservations)
                            + ". Hãy xử lý các lịch này trước hoặc giữ lại ghế cho đến khi sinh viên đổi/hủy chỗ.");
        }
    }

    private List<slib.com.example.entity.booking.ReservationEntity> findCurrentReservations(Set<Integer> seatIds,
                                                                                            LocalDateTime now) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }
        return reservationRepository.findCurrentReservationsForSeats(
                new ArrayList<>(seatIds),
                List.of("PROCESSING", "BOOKED", "CONFIRMED"),
                now);
    }

    private List<slib.com.example.entity.booking.ReservationEntity> findFutureReservations(Set<Integer> seatIds,
                                                                                           LocalDateTime now) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }
        return reservationRepository.findFutureReservationsForSeats(
                new ArrayList<>(seatIds),
                List.of("PROCESSING", "BOOKED", "CONFIRMED"),
                now);
    }

    private String summarizeSeats(List<slib.com.example.entity.booking.ReservationEntity> reservations) {
        return reservations.stream()
                .map(reservation -> reservation.getSeat() != null ? reservation.getSeat().getSeatCode() : null)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .collect(Collectors.joining(", "));
    }

    private boolean hasSeatStructureChanged(SeatResponse currentSeat, SeatResponse nextSeat) {
        return !Objects.equals(currentSeat.getZoneId(), nextSeat.getZoneId())
                || !Objects.equals(currentSeat.getSeatCode(), nextSeat.getSeatCode())
                || !Objects.equals(currentSeat.getRowNumber(), nextSeat.getRowNumber())
                || !Objects.equals(currentSeat.getColumnNumber(), nextSeat.getColumnNumber())
                || !Objects.equals(currentSeat.getIsActive(), nextSeat.getIsActive());
    }

    private boolean hasZoneStructureChanged(ZoneResponse currentZone, ZoneResponse nextZone) {
        return !Objects.equals(currentZone.getAreaId(), nextZone.getAreaId())
                || !Objects.equals(currentZone.getZoneName(), nextZone.getZoneName())
                || !Objects.equals(currentZone.getPositionX(), nextZone.getPositionX())
                || !Objects.equals(currentZone.getPositionY(), nextZone.getPositionY())
                || !Objects.equals(currentZone.getWidth(), nextZone.getWidth())
                || !Objects.equals(currentZone.getHeight(), nextZone.getHeight());
    }

    private boolean hasAreaStructureChanged(AreaResponse currentArea, AreaResponse nextArea) {
        return !Objects.equals(currentArea.getAreaName(), nextArea.getAreaName())
                || !Objects.equals(currentArea.getPositionX(), nextArea.getPositionX())
                || !Objects.equals(currentArea.getPositionY(), nextArea.getPositionY())
                || !Objects.equals(currentArea.getWidth(), nextArea.getWidth())
                || !Objects.equals(currentArea.getHeight(), nextArea.getHeight())
                || !Objects.equals(currentArea.getIsActive(), nextArea.getIsActive());
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

        validateDuplicateNames(snapshot, conflicts);
        validateAreaOverlaps(snapshot.getAreas(), conflicts);
        validateZones(snapshot.getZones(), areasById, conflicts);
        validateFactories(snapshot.getFactories(), areasById, snapshot.getZones(), conflicts);
        validateSeats(snapshot.getSeats(), zonesById, conflicts);

        boolean draftValid = conflicts.stream().noneMatch(conflict -> "error".equalsIgnoreCase(conflict.getSeverity()));
        if (draftValid) {
            LayoutSnapshotRequest currentSnapshot = buildCurrentSnapshot();
            LayoutChangeImpact impact = analyzeLayoutChangeImpact(currentSnapshot, snapshot);
            conflicts.addAll(buildPublishReadinessConflicts(impact, LocalDateTime.now(VIETNAM_ZONE)));
        }

        boolean publishable = draftValid
                && conflicts.stream().noneMatch(conflict -> "error".equalsIgnoreCase(conflict.getSeverity()));

        return LayoutValidationResponse.builder()
                .valid(draftValid)
                .publishable(publishable)
                .conflicts(conflicts)
                .build();
    }

    private List<LayoutConflictResponse> buildPublishReadinessConflicts(LayoutChangeImpact impact, LocalDateTime now) {
        List<LayoutConflictResponse> conflicts = new ArrayList<>();

        List<slib.com.example.entity.booking.ReservationEntity> currentReservations =
                findCurrentReservations(impact.blockedSeatIds(), now);
        if (!currentReservations.isEmpty()) {
            conflicts.add(conflict(
                    "PUBLISH_SEAT_IN_USE",
                    "error",
                    "layout",
                    "publish-readiness",
                    "Ghế đang được sử dụng hoặc giữ chỗ",
                    "Chưa thể xuất bản vì các ghế sau đang được sử dụng hoặc có lượt giữ chỗ đang hiệu lực: "
                            + summarizeSeats(currentReservations) + "."
            ));
        }

        List<slib.com.example.entity.booking.ReservationEntity> futureDestructiveReservations =
                findFutureReservations(impact.destructiveSeatIds(), now);
        if (!futureDestructiveReservations.isEmpty()) {
            conflicts.add(conflict(
                    "PUBLISH_FUTURE_BOOKING_IMPACT",
                    "error",
                    "layout",
                    "publish-readiness",
                    "Ghế đang có lịch đặt sắp tới",
                    "Chưa thể xuất bản vì các ghế sau có lịch đặt sắp tới và sẽ bị gỡ hoặc ngừng hoạt động: "
                            + summarizeSeats(futureDestructiveReservations) + "."
            ));
        }

        return conflicts;
    }

    private void validateDuplicateNames(LayoutSnapshotRequest snapshot, List<LayoutConflictResponse> conflicts) {
        Map<String, AreaResponse> areaNames = new HashMap<>();
        for (AreaResponse area : snapshot.getAreas()) {
            String normalizedName = safeName(area.getAreaName(), "").trim().toLowerCase(Locale.ROOT);
            if (normalizedName.isEmpty()) {
                continue;
            }

            AreaResponse existing = areaNames.putIfAbsent(normalizedName, area);
            if (existing != null) {
                conflicts.add(conflict("AREA_DUPLICATE_NAME", "error", "area", areaKey(area),
                        "Tên phòng thư viện đang bị trùng",
                        "Phòng \"" + safeName(area.getAreaName(), "Chưa đặt tên")
                                + "\" đang bị trùng tên với một phòng khác trong sơ đồ."));
            }
        }

        Map<Long, Map<String, ZoneResponse>> zoneNamesByArea = new HashMap<>();
        for (ZoneResponse zone : snapshot.getZones()) {
            Long areaId = zone.getAreaId() == null ? Long.MIN_VALUE : zone.getAreaId();
            String normalizedName = safeName(zone.getZoneName(), "").trim().toLowerCase(Locale.ROOT);
            if (normalizedName.isEmpty()) {
                continue;
            }

            Map<String, ZoneResponse> zoneNames = zoneNamesByArea.computeIfAbsent(areaId, ignored -> new HashMap<>());
            ZoneResponse existing = zoneNames.putIfAbsent(normalizedName, zone);
            if (existing != null) {
                conflicts.add(conflict("ZONE_DUPLICATE_NAME", "error", "zone", zoneKey(zone),
                        "Tên khu vực ghế đang bị trùng",
                        "Khu vực \"" + safeName(zone.getZoneName(), "Chưa đặt tên")
                                + "\" đang bị trùng tên với khu vực khác trong cùng phòng thư viện."));
            }
        }
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

        return new LayoutSnapshotRequest(null, areas, zones, seats, factories);
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
            return new LayoutSnapshotRequest(null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        return new LayoutSnapshotRequest(
                snapshot.getBasedOnPublishedVersion(),
                snapshot.getAreas() != null ? snapshot.getAreas() : new ArrayList<>(),
                snapshot.getZones() != null ? snapshot.getZones() : new ArrayList<>(),
                snapshot.getSeats() != null ? snapshot.getSeats() : new ArrayList<>(),
                snapshot.getFactories() != null ? snapshot.getFactories() : new ArrayList<>());
    }

    private LayoutSnapshotRequest readSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(resolveStoredText(snapshotJson, "layout snapshot"), LayoutSnapshotRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Không thể đọc dữ liệu nháp sơ đồ", e);
        }
    }

    private String resolveStoredText(String value, String fieldName) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || !trimmed.matches("^\\d+$")) {
            return value;
        }

        try {
            Object result = entityManager.createNativeQuery(
                            "SELECT convert_from(lo_get(CAST(:oid AS oid)), 'UTF8')")
                    .setParameter("oid", Long.parseLong(trimmed))
                    .getSingleResult();
            if (result instanceof String decoded && !decoded.isBlank()) {
                log.warn("Phát hiện dữ liệu legacy dạng OID ở {}: {}, tự động giải mã từ large object", fieldName, trimmed);
                return decoded;
            }
        } catch (Exception ex) {
            log.warn("Không thể giải mã dữ liệu legacy ở {} từ OID {}: {}", fieldName, trimmed, ex.getMessage());
        }

        return value;
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

    private ActorInfo resolveRequiredActor(Authentication authentication) {
        ActorInfo actor = resolveActor(authentication);
        if (actor.userId() == null) {
            throw new RuntimeException("Không thể xác định tài khoản thao tác sơ đồ");
        }
        return actor;
    }

    private long resolveBasedOnPublishedVersion(LayoutSnapshotRequest snapshot, Long fallbackVersion, long latestPublishedVersion) {
        if (snapshot != null && snapshot.getBasedOnPublishedVersion() != null) {
            return snapshot.getBasedOnPublishedVersion();
        }
        if (fallbackVersion != null) {
            return fallbackVersion;
        }
        return latestPublishedVersion;
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

    private String buildScheduleSummary(String prefix, LayoutSnapshotRequest snapshot, LocalDateTime scheduledFor) {
        return prefix + " vào " + formatScheduleTime(scheduledFor) + ". "
                + snapshot.getAreas().size() + " phòng, "
                + snapshot.getZones().size() + " khu vực, "
                + snapshot.getSeats().size() + " ghế, "
                + snapshot.getFactories().size() + " vật cản";
    }

    private String formatScheduleTime(LocalDateTime value) {
        if (value == null) {
            return "thời điểm không xác định";
        }
        return value.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
    }

    private String resolveScheduleFailureReason(LayoutValidationResponse validation) {
        if (validation == null) {
            return "Không thể xác định trạng thái lịch xuất bản";
        }
        return validation.getConflicts().stream()
                .filter(conflict -> "error".equalsIgnoreCase(conflict.getSeverity()))
                .map(LayoutConflictResponse::getMessage)
                .findFirst()
                .orElse("Sơ đồ không còn hợp lệ để xuất bản theo lịch");
    }

    private LayoutScheduleResponse toScheduleResponse(LayoutScheduleEntity entity) {
        if (entity == null) {
            return null;
        }
        return LayoutScheduleResponse.builder()
                .scheduleId(entity.getScheduleId())
                .basedOnPublishedVersion(entity.getBasedOnPublishedVersion())
                .scheduledFor(entity.getScheduledFor())
                .status(entity.getStatus())
                .requestedByName(entity.getRequestedByName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .cancelledAt(entity.getCancelledAt())
                .executedAt(entity.getExecutedAt())
                .lastError(entity.getLastError())
                .build();
    }

    private void publishScheduleChangedEvent() {
        eventPublisher.publishEvent(new LayoutScheduleChangedEvent());
    }

    private void cancelPendingSchedulesAfterImmediatePublish(ActorInfo actor, String actionType, LocalDateTime now) {
        if ("AUTO_PUBLISH".equals(actionType)) {
            return;
        }

        List<LayoutScheduleEntity> pendingSchedules =
                layoutScheduleRepository.findByStatusOrderByScheduledForAsc(SCHEDULE_STATUS_PENDING);
        if (pendingSchedules.isEmpty()) {
            return;
        }

        for (LayoutScheduleEntity schedule : pendingSchedules) {
            schedule.setStatus(SCHEDULE_STATUS_CANCELLED);
            schedule.setCancelledAt(now);
            schedule.setUpdatedAt(now);
            schedule.setLastError("Lịch đã bị hủy vì sơ đồ được xuất bản thủ công bởi "
                    + safeName(actor.displayName(), "người dùng khác") + ".");
        }
        layoutScheduleRepository.saveAll(pendingSchedules);
        publishScheduleChangedEvent();
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

    private record LayoutChangeImpact(Set<Integer> blockedSeatIds,
                                      Set<Integer> destructiveSeatIds,
                                      Set<Integer> warnOnlySeatIds) {
    }

    private record ActorInfo(UUID userId, String displayName) {
    }
}
