package cl.automata.vehiclecapture.web;

import cl.automata.vehiclecapture.domain.RecordStatus;
import cl.automata.vehiclecapture.service.Dtos.FileContent;
import cl.automata.vehiclecapture.service.Dtos.SchemaDto;
import cl.automata.vehiclecapture.service.Dtos.Stats;
import cl.automata.vehiclecapture.service.Dtos.UpdateRequest;
import cl.automata.vehiclecapture.service.Dtos.VehicleDetail;
import cl.automata.vehiclecapture.service.Dtos.VehicleSummary;
import cl.automata.vehiclecapture.service.VehicleService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VehicleController {

    private final VehicleService vehicles;

    public VehicleController(VehicleService vehicles) {
        this.vehicles = vehicles;
    }

    @GetMapping("/schema")
    public SchemaDto schema() {
        return vehicles.schema();
    }

    @GetMapping("/dashboard")
    public Stats dashboard() {
        return vehicles.stats();
    }

    @GetMapping("/vehicles")
    public List<VehicleSummary> list(@RequestParam(required = false) RecordStatus status,
                                     @RequestParam(required = false) String q) {
        return vehicles.list(status, q);
    }

    @GetMapping("/vehicles/{id}")
    public VehicleDetail get(@PathVariable UUID id) {
        return vehicles.get(id);
    }

    @PutMapping("/vehicles/{id}")
    public VehicleDetail update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest body) {
        return vehicles.update(id, body.data());
    }

    @PostMapping("/vehicles/{id}/validate")
    public VehicleDetail validate(@PathVariable UUID id) {
        return vehicles.validate(id);
    }

    @PostMapping("/vehicles/{id}/retry")
    public VehicleDetail retry(@PathVariable UUID id) {
        return vehicles.retry(id);
    }

    @GetMapping("/vehicles/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable UUID id) {
        FileContent f = vehicles.image(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.mimeType()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(f.fileName()).build().toString())
                .body(f.resource());
    }
}
