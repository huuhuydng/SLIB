package slib.com.example.scheduler;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import slib.com.example.entity.zone_config.LayoutScheduleEntity;
import slib.com.example.repository.zone_config.LayoutScheduleRepository;
import slib.com.example.service.zone_config.LayoutAdminService;
import slib.com.example.service.zone_config.LayoutScheduleChangedEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class LayoutPublishScheduler {

    private final LayoutScheduleRepository layoutScheduleRepository;
    private final LayoutAdminService layoutAdminService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        refreshPendingSchedules();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLayoutScheduleChanged(LayoutScheduleChangedEvent ignored) {
        refreshPendingSchedules();
    }

    public synchronized void refreshPendingSchedules() {
        List<LayoutScheduleEntity> pendingSchedules =
                layoutScheduleRepository.findByStatusOrderByScheduledForAsc(LayoutAdminService.SCHEDULE_STATUS_PENDING);
        scheduledTasks.keySet().forEach(this::cancelScheduledTask);

        for (LayoutScheduleEntity schedule : pendingSchedules) {
            scheduleTask(schedule);
        }
    }

    private void scheduleTask(LayoutScheduleEntity schedule) {
        Long scheduleId = schedule.getScheduleId();
        if (scheduleId == null || schedule.getScheduledFor() == null) {
            return;
        }
        if (scheduledTasks.containsKey(scheduleId)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(LayoutAdminService.VIETNAM_ZONE);
        long delayMillis = Math.max(0, Duration.between(now, schedule.getScheduledFor()).toMillis());

        log.info("[LayoutPublishScheduler] Scheduling layout publish {} at {} (in {} seconds)",
                scheduleId, schedule.getScheduledFor(), delayMillis / 1000);

        ScheduledFuture<?> future = scheduler.schedule(
                () -> executeScheduledPublish(scheduleId),
                delayMillis,
                TimeUnit.MILLISECONDS
        );
        scheduledTasks.put(scheduleId, future);
    }

    public void cancelScheduledTask(Long scheduleId) {
        ScheduledFuture<?> existingTask = scheduledTasks.remove(scheduleId);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
        }
    }

    private void executeScheduledPublish(Long scheduleId) {
        try {
            layoutAdminService.executeScheduledPublish(scheduleId);
        } catch (Exception ex) {
            log.error("[LayoutPublishScheduler] Scheduled publish {} failed", scheduleId, ex);
        } finally {
            scheduledTasks.remove(scheduleId);
            refreshPendingSchedules();
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
