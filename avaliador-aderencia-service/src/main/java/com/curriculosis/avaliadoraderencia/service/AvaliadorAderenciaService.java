package com.curriculosis.avaliadoraderencia.service;

import com.curriculosis.avaliadoraderencia.client.BackendOportunidadesClient;
import com.curriculosis.avaliadoraderencia.dto.AvaliacaoAderenciaResultado;
import com.curriculosis.avaliadoraderencia.dto.ExecucaoAvaliacaoResponse;
import com.curriculosis.avaliadoraderencia.dto.OportunidadeBackend;
import com.curriculosis.avaliadoraderencia.dto.StatusAvaliadorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvaliadorAderenciaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AvaliadorAderenciaService.class);

    private final BackendOportunidadesClient backendClient;
    private final OpenAiAderenciaService openAiAderenciaService;
    private final boolean schedulerHabilitado;
    private final String cron;
    private final int tamanhoLotePadrao;

    private Instant ultimaExecucaoInicio;
    private Instant ultimaExecucaoFim;
    private int ultimaQuantidadeAvaliada;

    public AvaliadorAderenciaService(
            BackendOportunidadesClient backendClient,
            OpenAiAderenciaService openAiAderenciaService,
            @Value("${avaliador.scheduler.enabled:true}") boolean schedulerHabilitado,
            @Value("${avaliador.scheduler.cron}") String cron,
            @Value("${avaliador.lote.tamanho:10}") int tamanhoLotePadrao
    ) {
        this.backendClient = backendClient;
        this.openAiAderenciaService = openAiAderenciaService;
        this.schedulerHabilitado = schedulerHabilitado;
        this.cron = cron;
        this.tamanhoLotePadrao = tamanhoLotePadrao;
    }

    @Scheduled(cron = "${avaliador.scheduler.cron}")
    public void executarAgendamento() {
        if (!schedulerHabilitado) {
            LOGGER.debug("Scheduler de aderência desabilitado.");
            return;
        }
        executar(tamanhoLotePadrao);
    }

    public synchronized ExecucaoAvaliacaoResponse executar(int limite) {
        Instant inicio = Instant.now();
        ultimaExecucaoInicio = inicio;
        LOGGER.info("Iniciando ciclo de avaliação de aderência com limite {}.", limite);

        List<OportunidadeBackend> oportunidades = backendClient.buscarOportunidadesPendentes(limite);
        List<AvaliacaoAderenciaResultado> resultados = new ArrayList<>();
        if (oportunidades != null) {
            for (OportunidadeBackend oportunidade : oportunidades) {
                AvaliacaoAderenciaResultado resultado = openAiAderenciaService.avaliar(oportunidade);
                resultados.add(resultado);
                try {
                    backendClient.enviarResultado(resultado);
                } catch (Exception e) {
                    LOGGER.warn("Não foi possível enviar resultado da oportunidade {} ao backend: {}", oportunidade.id(), e.getMessage());
                }
            }
        }

        Instant fim = Instant.now();
        ultimaExecucaoFim = fim;
        ultimaQuantidadeAvaliada = resultados.size();
        LOGGER.info("Ciclo de avaliação concluído. Avaliadas: {}.", resultados.size());
        return new ExecucaoAvaliacaoResponse(inicio, fim, oportunidades == null ? 0 : oportunidades.size(), resultados.size(), resultados);
    }

    public StatusAvaliadorResponse status() {
        return new StatusAvaliadorResponse(
                schedulerHabilitado,
                cron,
                tamanhoLotePadrao,
                ultimaExecucaoInicio,
                ultimaExecucaoFim,
                ultimaQuantidadeAvaliada
        );
    }
}
