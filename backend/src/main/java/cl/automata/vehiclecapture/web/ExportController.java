package cl.automata.vehiclecapture.web;

import cl.automata.vehiclecapture.service.Dtos.ExportDto;
import cl.automata.vehiclecapture.service.Dtos.ExportRequest;
import cl.automata.vehiclecapture.service.Dtos.FileContent;
import cl.automata.vehiclecapture.service.ExportService;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exports;

    public ExportController(ExportService exports) {
        this.exports = exports;
    }

    @GetMapping
    public List<ExportDto> history() {
        return exports.history();
    }

    /** Cuerpo opcional: {"recordIds": [...]}. Sin ids exporta todos los validados. */
    @PostMapping
    public ResponseEntity<ExportDto> create(@RequestBody(required = false) ExportRequest body) {
        ExportDto dto = exports.create(body == null ? null : body.recordIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        FileContent f = exports.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.fileName()).build().toString())
                .body(f.resource());
    }
}
