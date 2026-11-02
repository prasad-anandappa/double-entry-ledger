package dev.ledger.api;

import dev.ledger.application.ReconciliationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/reconciliations")
public class ReconciliationController {

    private final ReconciliationService reconciliations;

    public ReconciliationController(ReconciliationService reconciliations) {
        this.reconciliations = reconciliations;
    }

    /**
     * Uploads a bank-statement CSV and reconciles it against everything
     * posted in the ledger. Returns the persisted run, including every
     * exception (missing on either side, or amount mismatch).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportDtos.ReconciliationResponse> run(
            @RequestPart("file") MultipartFile file) throws IOException {
        var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        var run = reconciliations.runReconciliation(reader);
        return ResponseEntity.ok(ReportDtos.ReconciliationResponse.from(run));
    }

    @GetMapping("/{id}")
    public ReportDtos.ReconciliationResponse get(@PathVariable Long id) {
        return ReportDtos.ReconciliationResponse.from(reconciliations.getRun(id));
    }

    @GetMapping
    public List<ReportDtos.ReconciliationResponse> list() {
        return reconciliations.listRuns().stream()
                .map(ReportDtos.ReconciliationResponse::summary)
                .toList();
    }
}
