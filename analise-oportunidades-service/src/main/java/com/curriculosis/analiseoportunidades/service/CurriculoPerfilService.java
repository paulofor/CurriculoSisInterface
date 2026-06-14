package com.curriculosis.analiseoportunidades.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

@Service
public class CurriculoPerfilService {

    private final ObjectMapper objectMapper;
    private final Path curriculoPath;

    public CurriculoPerfilService(
            ObjectMapper objectMapper,
            @Value("${curriculo.master.path:docs/curriculos/paulo_forestieri_curriculo_master_inicial.json}") String curriculoPath
    ) {
        this.objectMapper = objectMapper;
        this.curriculoPath = Path.of(curriculoPath);
    }

    public String obterTextoCurriculo() {
        JsonNode root = lerCurriculo();
        StringBuilder texto = new StringBuilder();
        adicionarTexto(root, texto);
        return texto.toString();
    }

    public String obterReferencia() {
        JsonNode metadata = lerCurriculo().path("metadata");
        String arquivo = metadata.path("arquivo").asText(curriculoPath.getFileName().toString());
        String versao = metadata.path("versao").asText("");
        return versao.isBlank() ? arquivo : arquivo + " v" + versao;
    }

    private JsonNode lerCurriculo() {
        Path path = resolverPathExistente();
        try {
            return objectMapper.readTree(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o currículo mestre em " + path, e);
        }
    }

    private Path resolverPathExistente() {
        if (Files.exists(curriculoPath)) {
            return curriculoPath;
        }

        Path fallback = Path.of("..", curriculoPath.toString()).normalize();
        if (Files.exists(fallback)) {
            return fallback;
        }

        throw new IllegalStateException("Currículo mestre não encontrado em " + curriculoPath + " ou " + fallback);
    }

    private void adicionarTexto(JsonNode node, StringBuilder texto) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            texto.append(node.asText()).append(' ');
            return;
        }

        if (node.isArray()) {
            node.forEach(item -> adicionarTexto(item, texto));
            return;
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                texto.append(field.getKey()).append(' ');
                adicionarTexto(field.getValue(), texto);
            }
        }
    }
}
