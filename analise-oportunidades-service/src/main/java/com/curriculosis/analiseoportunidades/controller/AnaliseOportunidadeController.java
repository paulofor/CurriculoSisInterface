package com.curriculosis.analiseoportunidades.controller;

import com.curriculosis.analiseoportunidades.dto.OportunidadeRequest;
import com.curriculosis.analiseoportunidades.dto.OportunidadeResponse;
import com.curriculosis.analiseoportunidades.service.AnaliseOportunidadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oportunidades")
public class AnaliseOportunidadeController {

    private final AnaliseOportunidadeService service;

    public AnaliseOportunidadeController(AnaliseOportunidadeService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("analise-oportunidades-service online");
    }

    @PostMapping("/analisar")
    public ResponseEntity<OportunidadeResponse> analisar(@Valid @RequestBody OportunidadeRequest request) {
        return ResponseEntity.ok(service.analisar(request));
    }
}
