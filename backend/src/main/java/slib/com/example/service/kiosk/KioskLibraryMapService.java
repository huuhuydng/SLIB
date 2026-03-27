package slib.com.example.service.kiosk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.kiosk.KioskLibraryMapDTO;
import slib.com.example.dto.kiosk.KioskZoneMapDTO;
import slib.com.example.entity.kiosk.KioskLibraryMapEntity;
import slib.com.example.entity.kiosk.KioskZoneMapEntity;
import slib.com.example.repository.kiosk.KioskLibraryMapRepository;
import slib.com.example.repository.kiosk.KioskZoneMapRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing library map and zones
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KioskLibraryMapService {

    private final KioskLibraryMapRepository libraryMapRepository;
    private final KioskZoneMapRepository zoneMapRepository;

    /**
     * Get the active library map with all zones
     */
    @Transactional(readOnly = true)
    public Optional<KioskLibraryMapDTO> getActiveMap() {
        return libraryMapRepository.findByIsActiveTrue()
                .map(this::mapToDTO);
    }

    /**
     * Get library map by ID
     */
    @Transactional(readOnly = true)
    public Optional<KioskLibraryMapDTO> getMapById(Integer id) {
        return libraryMapRepository.findById(id)
                .map(this::mapToDTO);
    }

    /**
     * Get all library maps
     */
    @Transactional(readOnly = true)
    public List<KioskLibraryMapDTO> getAllMaps() {
        return libraryMapRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new library map
     */
    @Transactional
    public KioskLibraryMapDTO createMap(KioskLibraryMapDTO dto) {
        log.info("Creating new library map: {}", dto.getMapName());

        // Set other maps to inactive if this is active
        if (Boolean.TRUE.equals(dto.getIsActive())) {
            libraryMapRepository.findAll().forEach(map -> {
                if (Boolean.TRUE.equals(map.getIsActive())) {
                    map.setIsActive(false);
                    libraryMapRepository.save(map);
                }
            });
        }

        KioskLibraryMapEntity entity = KioskLibraryMapEntity.builder()
                .mapName(dto.getMapName())
                .mapImageUrl(dto.getMapImageUrl())
                .publicId(extractPublicId(dto.getMapImageUrl()))
                .description(dto.getDescription())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        KioskLibraryMapEntity saved = libraryMapRepository.save(entity);
        log.info("✅ Library map created with ID: {}", saved.getId());

        return mapToDTO(saved);
    }

    /**
     * Update an existing library map
     */
    @Transactional
    public KioskLibraryMapDTO updateMap(Integer id, KioskLibraryMapDTO dto) {
        log.info("Updating library map ID: {}", id);

        KioskLibraryMapEntity entity = libraryMapRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Library map not found with ID: " + id));

        entity.setMapName(dto.getMapName());
        entity.setMapImageUrl(dto.getMapImageUrl());
        entity.setPublicId(extractPublicId(dto.getMapImageUrl()));
        entity.setDescription(dto.getDescription());

        if (Boolean.TRUE.equals(dto.getIsActive()) && !Boolean.TRUE.equals(entity.getIsActive())) {
            // Set other maps to inactive
            libraryMapRepository.findAll().forEach(map -> {
                if (!map.getId().equals(id) && Boolean.TRUE.equals(map.getIsActive())) {
                    map.setIsActive(false);
                    libraryMapRepository.save(map);
                }
            });
            entity.setIsActive(true);
        } else {
            entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false);
        }

        KioskLibraryMapEntity updated = libraryMapRepository.save(entity);
        log.info("✅ Library map updated: {}", id);

        return mapToDTO(updated);
    }

    /**
     * Delete library map
     */
    @Transactional
    public void deleteMap(Integer id) {
        log.info("Deleting library map ID: {}", id);

        if (!libraryMapRepository.existsById(id)) {
            throw new RuntimeException("Library map not found with ID: " + id);
        }

        libraryMapRepository.deleteById(id);
        log.info("✅ Library map deleted: {}", id);
    }

    /**
     * Add a zone to library map
     */
    @Transactional
    public KioskZoneMapDTO addZoneToMap(Integer mapId, KioskZoneMapDTO dto) {
        log.info("Adding zone to map ID: {}", mapId);

        KioskLibraryMapEntity map = libraryMapRepository.findById(mapId)
                .orElseThrow(() -> new RuntimeException("Library map not found with ID: " + mapId));

        KioskZoneMapEntity zone = KioskZoneMapEntity.builder()
                .libraryMap(map)
                .zoneName(dto.getZoneName())
                .zoneType(dto.getZoneType())
                .xPosition(dto.getXPosition())
                .yPosition(dto.getYPosition())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .colorCode(dto.getColorCode())
                .isInteractive(dto.getIsInteractive() != null ? dto.getIsInteractive() : true)
                .build();

        KioskZoneMapEntity saved = zoneMapRepository.save(zone);
        log.info("✅ Zone added to map: {}", saved.getId());

        return zoneMapToDTO(saved);
    }

    /**
     * Update a zone
     */
    @Transactional
    public KioskZoneMapDTO updateZone(Integer zoneId, KioskZoneMapDTO dto) {
        log.info("Updating zone ID: {}", zoneId);

        KioskZoneMapEntity zone = zoneMapRepository.findById(zoneId)
                .orElseThrow(() -> new RuntimeException("Zone not found with ID: " + zoneId));

        zone.setZoneName(dto.getZoneName());
        zone.setZoneType(dto.getZoneType());
        zone.setXPosition(dto.getXPosition());
        zone.setYPosition(dto.getYPosition());
        zone.setWidth(dto.getWidth());
        zone.setHeight(dto.getHeight());
        zone.setColorCode(dto.getColorCode());
        zone.setIsInteractive(dto.getIsInteractive() != null ? dto.getIsInteractive() : true);

        KioskZoneMapEntity updated = zoneMapRepository.save(zone);
        log.info("✅ Zone updated: {}", zoneId);

        return zoneMapToDTO(updated);
    }

    /**
     * Delete a zone
     */
    @Transactional
    public void deleteZone(Integer zoneId) {
        log.info("Deleting zone ID: {}", zoneId);

        if (!zoneMapRepository.existsById(zoneId)) {
            throw new RuntimeException("Zone not found with ID: " + zoneId);
        }

        zoneMapRepository.deleteById(zoneId);
        log.info("✅ Zone deleted: {}", zoneId);
    }

    /**
     * Convert entity to DTO
     */
    private KioskLibraryMapDTO mapToDTO(KioskLibraryMapEntity entity) {
        return KioskLibraryMapDTO.builder()
                .id(entity.getId())
                .mapName(entity.getMapName())
                .mapImageUrl(entity.getMapImageUrl())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .zones(entity.getZones() != null ? entity.getZones().stream()
                        .map(this::zoneMapToDTO)
                        .collect(Collectors.toList()) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Convert zone entity to DTO
     */
    private KioskZoneMapDTO zoneMapToDTO(KioskZoneMapEntity entity) {
        return KioskZoneMapDTO.builder()
                .id(entity.getId())
                .zoneName(entity.getZoneName())
                .zoneType(entity.getZoneType())
                .xPosition(entity.getXPosition())
                .yPosition(entity.getYPosition())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .colorCode(entity.getColorCode())
                .isInteractive(entity.getIsInteractive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Extract public ID from Cloudinary URL
     */
    private String extractPublicId(String url) {
        if (url == null || !url.contains("/")) {
            return null;
        }
        // Extract from URL like: https://res.cloudinary.com/dhhy1g4vk/image/upload/v1/folder/filename
        String[] parts = url.split("/upload/");
        if (parts.length > 1) {
            return parts[1].replaceAll("\\.[^.]+$", ""); // Remove file extension
        }
        return null;
    }
}
