package slib.com.example.controller.zone_config;

import slib.com.example.dto.zone_config.AreaFactoryResponse;
import slib.com.example.entity.zone_config.AreaFactoryEntity;
import slib.com.example.service.zone_config.AreaFactoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/slib/area_factories")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class AreaFactoryController {

    private final AreaFactoryService areaFactoryService;

    @GetMapping
    public List<AreaFactoryResponse> getAll() {
        return areaFactoryService.getAllAreaFactories();
    }

    @GetMapping("/area/{areaId}")
    public List<AreaFactoryResponse> getByArea(@PathVariable Long areaId) {
        return areaFactoryService.getFactoriesByAreaId(areaId);
    }

    @GetMapping("/{id}")
    public AreaFactoryResponse getById(@PathVariable Long id) {
        return areaFactoryService.getAreaFactoryById(id);
    }

    @PostMapping("/area/{areaId}")
    public AreaFactoryResponse create(
            @PathVariable Long areaId,
            @RequestBody AreaFactoryEntity entity
    ) {
        return areaFactoryService.createAreaFactory(areaId, entity);
    }

    @PutMapping("/{id}")
    public AreaFactoryResponse update(
            @PathVariable Long id,
            @RequestBody AreaFactoryEntity entity
    ) {
        return areaFactoryService.updateAreaFactory(id, entity);
    }

    @PatchMapping("/{id}/drag")
    public AreaFactoryResponse drag(
            @PathVariable Long id,
            @RequestParam Integer x,
            @RequestParam Integer y
    ) {
        return areaFactoryService.dragAreaFactory(id, x, y);
    }

    @PatchMapping("/{id}/resize")
    public AreaFactoryResponse resize(
            @PathVariable Long id,
            @RequestParam Integer width,
            @RequestParam Integer height
    ) {
        return areaFactoryService.resizeAreaFactory(id, width, height);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        areaFactoryService.deleteAreaFactory(id);
    }
}
