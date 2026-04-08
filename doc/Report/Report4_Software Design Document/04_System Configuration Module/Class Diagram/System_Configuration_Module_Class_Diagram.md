# System Configuration Module Class Diagram

```mermaid
classDiagram
    class AreaController {
        +getAllAreas()
        +getAreaById(id)
        +createArea(request)
        +updateArea(id, request)
        +updateAreaLocked(id, request)
        +updateAreaIsActive(id, request)
        +deleteArea(id)
    }

    class ZoneController {
        +getAllZones(areaId)
        +getZoneById(id)
        +createZone(request)
        +updateZone(id, request)
        +updatePosition(id, request)
        +updateDimensions(id, request)
        +deleteZone(id)
    }

    class SeatController {
        +createSeat(request)
        +getAllSeats(zoneId)
        +getSeatById(id)
        +updateSeat(id, request)
        +deleteSeat(id)
        +updateSeatNfcUid(id, request)
        +clearSeatNfcUid(id)
        +getNfcMappings(filters)
        +getSeatNfcInfo(id)
    }

    class AmenityController {
        +getAmenities(zoneId)
        +getAmenityById(id)
        +createAmenity(request)
        +updateAmenity(id, request)
        +deleteAmenity(id)
    }

    class AreaService {
        +getAllAreas()
        +getAreaById(id)
        +createArea(request)
        +updateArea(id, request)
        +updateLocked(id, request)
        +updateActive(id, request)
        +deleteArea(id)
    }

    class ZoneService {
        +getAllZones(areaId)
        +getZoneById(id)
        +createZone(request)
        +updateZone(id, request)
        +updatePosition(id, request)
        +updateDimensions(id, request)
        +deleteZone(id)
    }

    class SeatService {
        +createSeat(request)
        +getAllSeats(zoneId)
        +getSeatById(id)
        +updateSeat(id, request)
        +deleteSeat(id)
        +updateSeatNfcUid(id, request)
        +clearSeatNfcUid(id)
        +getNfcMappings(filters)
        +getSeatNfcInfo(id)
    }

    class AmenityService {
        +getAmenitiesByZoneId(zoneId)
        +getAmenityById(id)
        +createAmenity(request)
        +updateAmenity(id, request)
        +deleteAmenity(id)
    }

    class ReputationRuleController {
        +getAllRules()
        +getRuleById(id)
        +createRule(request)
        +updateRule(id, request)
        +toggleRuleStatus(id)
        +deleteRule(id)
    }

    class LibrarySettingController {
        +getSettings()
        +updateSettings(dto)
        +toggleLock(body)
        +resetSettings()
        +getTimeSlots()
    }

    class HceStationController {
        +getAllStations(search, status, deviceType)
        +getStationById(id)
        +createStation(request)
        +updateStation(id, request)
        +updateStationStatus(id, request)
        +deleteStation(id)
    }

    class MaterialController {
        +getAllMaterials()
        +getMaterialById(id)
        +createMaterial(request, user)
        +updateMaterial(id, request, user)
        +deleteMaterial(id, user)
        +getItems(id)
        +addTextItem(id, request, user)
        +addFileItem(id, name, file, user)
        +updateItem(materialId, itemId, request, user)
        +deleteItem(materialId, itemId, user)
    }

    class KnowledgeStoreController {
        +getAllKnowledgeStores()
        +getKnowledgeStoreById(id)
        +createKnowledgeStore(request, user)
        +updateKnowledgeStore(id, request, user)
        +deleteKnowledgeStore(id, user)
        +syncKnowledgeStore(id, user)
    }

    class BackupController {
        +triggerBackup(user)
        +getHistory()
        +downloadBackup(id)
        +getSchedule()
        +updateSchedule(request, user)
    }

    class SystemInfoController {
        +getSystemInfo()
    }

    class SystemLogController {
        +getLogs(filters)
        +getStats(startDate, endDate)
        +exportLogs(filters)
        +cleanupLogs(request)
    }

    class MaterialService {
        +getAllMaterials()
        +getMaterialById(id)
        +createMaterial(request, createdBy)
        +updateMaterial(id, request)
        +deleteMaterial(id)
        +getItemsByMaterialId(id)
        +addTextItem(id, request)
        +addFileItem(id, name, file)
        +updateItem(materialId, itemId, request)
        +deleteItem(materialId, itemId)
    }

    class KnowledgeStoreService {
        +getAllKnowledgeStores()
        +getKnowledgeStoreById(id)
        +createKnowledgeStore(request, createdBy)
        +updateKnowledgeStore(id, request)
        +deleteKnowledgeStore(id)
        +syncKnowledgeStore(id)
    }

    class LibrarySettingService {
        +getSettingsDTO()
        +updateSettings(dto)
        +toggleLibraryClosed(closed, reason)
        +resetSettings()
        +generateTimeSlots()
    }

    class HceStationService {
        +getAllStations(search, status, deviceType)
        +getStationById(id)
        +createStation(request)
        +updateStation(id, request)
        +updateStationStatus(id, request)
        +deleteStation(id)
        +processHeartbeat(deviceId)
    }

    class BackupService {
        +performBackup()
        +getHistory()
        +getBackupFile(id)
    }

    class SystemLogService {
        +getLogs(filters)
        +getStats(startDate, endDate)
        +exportLogsToExcel(filters)
        +cleanupLogsBefore(beforeDate)
        +logAudit(service, message, detail, actor)
        +logError(service, message, detail)
    }

    class KioskSlideshowController {
        +getImages()
        +getConfig()
        +uploadImages(files)
        +deleteImage(id)
        +renameImage(id, payload)
        +toggleStatus(id, payload)
        +reorderImages(orderedIds)
    }

    class KioskCloudinaryService {
        +uploadSlideShowImage(file)
        +deleteSlideShowImage(publicId)
    }

    class AreaRepository {
        +findAll()
        +findById(id)
        +save(area)
        +delete(area)
    }

    class ZoneRepository {
        +findAll()
        +findById(id)
        +save(zone)
        +delete(zone)
    }

    class SeatRepository {
        +findAll()
        +findById(id)
        +save(seat)
        +delete(seat)
    }

    class AmenityRepository {
        +findAll()
        +findById(id)
        +save(amenity)
        +delete(amenity)
    }

    class ReputationRuleRepository {
        +findAll()
        +findById(id)
        +findByRuleCode(ruleCode)
        +existsById(id)
        +save(rule)
        +deleteById(id)
    }

    class LibrarySettingRepository {
        +findAll()
        +save(setting)
    }

    class HceDeviceRepository {
        +findAll()
        +findById(id)
        +save(device)
        +delete(device)
    }

    class MaterialRepository {
        +findAll()
        +findById(id)
        +save(material)
        +delete(material)
    }

    class MaterialItemRepository {
        +findAll()
        +findById(id)
        +save(item)
        +delete(item)
    }

    class KnowledgeStoreRepository {
        +findAll()
        +findById(id)
        +save(store)
        +delete(store)
    }

    class BackupHistoryRepository {
        +findAll()
        +save(history)
        +findById(id)
    }

    class BackupScheduleRepository {
        +findFirstByOrderByIdAsc()
        +save(schedule)
    }

    class SystemLogRepository {
        +findAll(specification, pageable)
        +save(log)
        +deleteAll()
    }

    class KioskImageRepository {
        +findAllByOrderByDisplayOrderAscCreatedAtDesc()
        +findById(id)
        +save(image)
        +delete(image)
    }

    class AreaEntity {
        +Long areaId
        +String areaName
        +Boolean locked
        +Boolean isActive
        +Integer width
        +Integer height
    }

    class ZoneEntity {
        +Integer zoneId
        +String zoneName
        +Boolean isLocked
        +String color
        +Integer width
        +Integer height
    }

    class SeatEntity {
        +Integer seatId
        +String seatCode
        +Boolean isActive
        +String nfcTagUid
        +Integer positionX
        +Integer positionY
    }

    class AmenityEntity {
        +Integer amenityId
        +String amenityName
    }

    class ReputationRuleEntity {
        +Integer ruleId
        +String ruleCode
        +String ruleName
        +Integer points
        +Boolean isActive
    }

    class LibrarySetting {
        +Integer id
        +LocalTime openTime
        +LocalTime closeTime
        +Boolean autoCheckoutEnabled
        +Boolean libraryClosed
    }

    class HceDeviceEntity {
        +Integer id
        +String deviceId
        +String deviceName
        +String deviceType
        +String status
        +Boolean online
    }

    class MaterialEntity {
        +Long id
        +String name
        +String description
        +Boolean active
    }

    class MaterialItemEntity {
        +Long id
        +String name
        +String type
        +String content
        +String fileName
    }

    class KnowledgeStoreEntity {
        +Long id
        +String name
        +String description
        +String status
    }

    class KioskImageEntity {
        +Integer id
        +String imageUrl
        +String publicId
        +String imageName
        +Boolean isActive
        +Integer displayOrder
    }

    class BackupHistoryEntity {
        +UUID id
        +String filePath
        +Long fileSizeBytes
        +String status
        +LocalDateTime startedAt
        +LocalDateTime completedAt
    }

    class BackupScheduleEntity {
        +Integer id
        +String scheduleName
        +String cronExpression
        +Integer retainDays
        +Boolean isActive
        +LocalDateTime nextBackupAt
    }

    class SystemLogEntity {
        +UUID id
        +String level
        +String category
        +String message
        +LocalDateTime createdAt
    }

    AreaController --> AreaService
    ZoneController --> ZoneService
    SeatController --> SeatService
    AmenityController --> AmenityService
    ReputationRuleController --> ReputationRuleRepository
    LibrarySettingController --> LibrarySettingService
    HceStationController --> HceStationService
    MaterialController --> MaterialService
    KnowledgeStoreController --> KnowledgeStoreService
    BackupController --> BackupService
    BackupController --> BackupScheduleRepository
    SystemLogController --> SystemLogService
    KioskSlideshowController --> KioskCloudinaryService
    KioskSlideshowController --> KioskImageRepository

    AreaService --> AreaRepository
    ZoneService --> ZoneRepository
    SeatService --> SeatRepository
    AmenityService --> AmenityRepository
    LibrarySettingService --> LibrarySettingRepository
    HceStationService --> HceDeviceRepository
    MaterialService --> MaterialRepository
    MaterialService --> MaterialItemRepository
    KnowledgeStoreService --> KnowledgeStoreRepository
    BackupService --> BackupHistoryRepository
    SystemLogService --> SystemLogRepository

    AreaRepository --> AreaEntity
    ZoneRepository --> ZoneEntity
    SeatRepository --> SeatEntity
    AmenityRepository --> AmenityEntity
    ReputationRuleRepository --> ReputationRuleEntity
    LibrarySettingRepository --> LibrarySetting
    HceDeviceRepository --> HceDeviceEntity
    MaterialRepository --> MaterialEntity
    MaterialItemRepository --> MaterialItemEntity
    KnowledgeStoreRepository --> KnowledgeStoreEntity
    KioskImageRepository --> KioskImageEntity
    BackupHistoryRepository --> BackupHistoryEntity
    BackupScheduleRepository --> BackupScheduleEntity
    SystemLogRepository --> SystemLogEntity

    AreaEntity "1" --> "many" ZoneEntity : contains
    ZoneEntity "1" --> "many" SeatEntity : contains
    ZoneEntity "1" --> "many" AmenityEntity : has
    MaterialEntity "1" --> "many" MaterialItemEntity : contains
    KnowledgeStoreEntity "many" --> "many" MaterialItemEntity : groups
```
