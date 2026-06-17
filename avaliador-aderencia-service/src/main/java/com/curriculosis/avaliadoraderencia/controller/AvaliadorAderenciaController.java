package com.curriculosis.avaliadoraderencia.controller;

import com.curriculosis.avaliadoraderencia.dto.ExecucaoAvaliacaoResponse;
import com.curriculosis.avaliadoraderencia.dto.ExecutarAvaliacaoRequest;
import com.curriculosis.avaliadoraderencia.dto.StatusAvaliadorResponse;
import com.curriculosis.avaliadoraderencia.service.AvaliadorAderenciaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/avaliador-aderencia")
public class AvaliadorAderenciaController {

    private final AvaliadorAderenciaService service;
    private final int tamanhoLotePadrao;

    public AvaliadorAderenciaController(
            AvaliadorAderenciaService service,
            @Value("${avaliador.lote.tamanho:10}") int tamanhoLotePadrao
    ) {
        this.service = service;
        this.tamanhoLotePadrao = tamanhoLotePadrao;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("avaliador-aderencia-service online");
    }

    @GetMapping("/status")
    public ResponseEntity<StatusAvaliadorResponse> status() {
        return ResponseEntity.ok(service.status());
    }

    @PostMapping("/executar")
    public ResponseEntity<ExecucaoAvaliacaoResponse> executar(@Valid @RequestBody(required = false) ExecutarAvaliacaoRequest request) {
        int limite = request == null ? tamanhoLotePadrao : request.limiteOuPadrao(tamanhoLotePadrao);
        return ResponseEntity.ok(service.executar(limite));
    }
}
