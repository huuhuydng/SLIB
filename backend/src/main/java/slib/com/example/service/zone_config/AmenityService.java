package slib.com.example.service.zone_config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import slib.com.example.dto.zone_config.AmenityResponse;
import slib.com.example.entity.zone_config.AmenityEntity;
import slib.com.example.entity.zone_config.ZoneEntity;
import slib.com.example.repository.zone_config.AmenityRepository;
import slib.com.example.repository.zone_config.ZoneRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final ZoneRepository zoneRepository;

    // GET amenities theo zoneId
    public List<AmenityResponse> getAmenitiesByZoneId(Integer zoneId) {
        return amenityRepository.findByZone_ZoneId(zoneId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET ALL
    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // GET BY ID
    public AmenityResponse getAmenityById(Integer id) {
        AmenityEntity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));
        return toResponse(amenity);
    }

    // CREATE
    public AmenityResponse createAmenity(AmenityResponse req) {
        ZoneEntity zone = zoneRepository.findById(req.getZoneId())
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        AmenityEntity amenity = AmenityEntity.builder()
                .zone(zone)
                .amenityName(req.getAmenityName())
                .build();

        return toResponse(amenityRepository.save(amenity));
    }

    // UPDATE
    public AmenityResponse updateAmenity(Integer id, AmenityResponse req) {
        AmenityEntity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        amenity.setAmenityName(req.getAmenityName());

        return toResponse(amenityRepository.save(amenity));
    }

    // DELETE
    public void deleteAmenity(Integer id) {
        if (!amenityRepository.existsById(id)) {
            throw new RuntimeException("Amenity not found");
        }
        amenityRepository.deleteById(id);
    }

    // MAP ENTITY -> DTO
    private AmenityResponse toResponse(AmenityEntity amenity) {
        AmenityResponse res = new AmenityResponse();
        res.setAmenityId(amenity.getAmenityId());
        res.setZoneId(amenity.getZone().getZoneId());
        res.setAmenityName(amenity.getAmenityName());
        return res;
    }
}