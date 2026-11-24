package cl.automata.vehiclecapture.web;

import cl.automata.vehiclecapture.service.CaptureService;
import cl.automata.vehiclecapture.service.Dtos.CaptureResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/captures")
public class CaptureController {

    private final CaptureService captures;

    public CaptureController(CaptureService captures) {
        this.captures = captures;
    }

    /** 201 si se creó un registro nuevo; 200 si esa misma foto ya estaba cargada (idempotente). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CaptureResult> capture(@RequestParam("file") MultipartFile file) {
        CaptureResult result = captures.capture(file);
        return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.CREATED).body(result);
    }
}
