package com.curriculosis.avaliadoraderencia.service;

import org.springframework.stereotype.Service;

@Service
public class CurriculoReferenciaService {

    public String obterResumoCurriculo() {
        return "Profissional sênior com experiência em Java, Spring Boot, APIs REST/SOAP, integrações, " +
                "dados, Python, SQL, Hadoop/Spark e IA Generativa/LLMs. Busca oportunidades remotas em " +
                "IA aplicada, backend, dados ou arquitetura. Restrição importante: não considerar como alta " +
                "aderência vagas que exijam inglês fluente.";
    }
}
