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

import java.time.Duration;
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
        LOGGER.info("Disparo do scheduler de aderência recebido. habilitado={}, cron={}, tamanhoLotePadrao={}", schedulerHabilitado, cron, tamanhoLotePadrao);
        if (!schedulerHabilitado) {
            LOGGER.debug("Scheduler de aderência desabilitado.");
            return;
        }
        LOGGER.info("Execução agendada do avaliador de aderência iniciada. cron={}, tamanhoLote={}", cron, tamanhoLotePadrao);
        executar(tamanhoLotePadrao);
    }

    public synchronized ExecucaoAvaliacaoResponse executar(int limite) {
        Instant inicio = Instant.now();
        ultimaExecucaoInicio = inicio;
        LOGGER.info("Execução do avaliador de aderência iniciada. limite={}, inicio={}", limite, inicio);
        LOGGER.info("Iniciando ciclo de avaliação de aderência. limite={}, inicio={}", limite, inicio);

        List<OportunidadeBackend> oportunidades = null;
        List<AvaliacaoAderenciaResultado> resultados = new ArrayList<>();
        try {
            oportunidades = backendClient.buscarOportunidadesPendentes(limite);
            if (oportunidades == null || oportunidades.isEmpty()) {
                LOGGER.info("Nenhuma oportunidade pendente retornada pelo backend para o ciclo atual.");
            } else {
                LOGGER.info("Iniciando processamento de {} oportunidades retornadas pelo backend.", oportunidades.size());
                for (int indice = 0; indice < oportunidades.size(); indice++) {
                    OportunidadeBackend oportunidade = oportunidades.get(indice);
                    LOGGER.info(
                            "Avaliando oportunidade {}/{}. oportunidadeId={}, titulo={}, empresa={}",
                            indice + 1, oportunidades.size(), oportunidade.id(), oportunidade.titulo(), oportunidade.empresa()
                    );
                    AvaliacaoAderenciaResultado resultado = openAiAderenciaService.avaliar(oportunidade);
                    resultados.add(resultado);
                    LOGGER.info(
                            "Avaliação concluída. oportunidadeId={}, status={}, nota={}",
                            oportunidade.id(), resultado.status(), resultado.notaAderencia()
                    );
                    try {
                        backendClient.enviarResultado(resultado);
                    } catch (Exception e) {
                        LOGGER.warn("Não foi possível enviar resultado da oportunidade {} ao backend: {}", oportunidade.id(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Ciclo de avaliação falhou antes da conclusão. limite={}, avaliadasAteFalha={}, erro={}", limite, resultados.size(), e.getMessage(), e);
            throw e;
        } finally {
            Instant fim = Instant.now();
            ultimaExecucaoFim = fim;
            ultimaQuantidadeAvaliada = resultados.size();
            LOGGER.info(
                    "Ciclo de avaliação finalizado. limite={}, oportunidadesRecebidas={}, avaliadas={}, inicio={}, fim={}, duracaoMs={}",
                    limite,
                    oportunidades == null ? 0 : oportunidades.size(),
                    resultados.size(),
                    inicio,
                    fim,
                    Duration.between(inicio, fim).toMillis()
            );
        }
        return new ExecucaoAvaliacaoResponse(inicio, ultimaExecucaoFim, oportunidades == null ? 0 : oportunidades.size(), resultados.size(), resultados);
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
