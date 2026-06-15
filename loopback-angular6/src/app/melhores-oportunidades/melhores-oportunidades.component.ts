import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { OportunidadeLinkedin, OportunidadeLinkedinApi } from '../shared/sdk';
import { ANALISE_OPORTUNIDADES_URL } from '../constantes/base.url';

interface OportunidadeCurriculoResponse {
  titulo: string;
  empresa: string;
  scoreFinal: number;
  recomendacao: string;
  termosAderentes: string[];
  lacunas: string[];
  analiseIa: string;
}

interface RankingOportunidadesCurriculoResponse {
  curriculoReferencia: string;
  totalOportunidades: number;
  oportunidades: OportunidadeCurriculoResponse[];
}

@Component({
  selector: 'app-melhores-oportunidades',
  templateUrl: './melhores-oportunidades.component.html',
  styleUrls: ['./melhores-oportunidades.component.css']
})
export class MelhoresOportunidadesComponent implements OnInit {
  carregando = false;
  erro = '';
  curriculoReferencia = '';
  totalOportunidades = 0;
  oportunidades: OportunidadeCurriculoResponse[] = [];
  oportunidadesIgnoradas: OportunidadeCurriculoResponse[] = [];

  constructor(
    private oportunidadeLinkedinApi: OportunidadeLinkedinApi,
    private http: HttpClient
  ) { }

  ngOnInit() {
    this.carregarMelhoresOportunidades();
  }

  carregarMelhoresOportunidades() {
    this.carregando = true;
    this.erro = '';
    this.oportunidades = [];
    this.oportunidadesIgnoradas = [];

    this.oportunidadeLinkedinApi.find(this.getFiltroOportunidades())
      .subscribe(
        (oportunidades: OportunidadeLinkedin[]) => this.analisarOportunidades(oportunidades),
        erro => this.tratarErro('Não foi possível carregar as oportunidades do LinkedIn.', erro)
      );
  }

  private analisarOportunidades(oportunidades: OportunidadeLinkedin[]) {
    if (!oportunidades || oportunidades.length === 0) {
      this.carregando = false;
      return;
    }

    var payload = {
      oportunidades: oportunidades.map(item => ({
        titulo: item.titulo || 'Oportunidade sem título',
        empresa: item.empresa || 'Empresa não informada',
        nivel: this.inferirNivel(item),
        descricaoOportunidade: item.descricao || item.titulo || '',
        compatibilidade: 40,
        salarioEstimado: 0
      }))
    };

    this.http.post<RankingOportunidadesCurriculoResponse>(ANALISE_OPORTUNIDADES_URL + '/aderentes-curriculo', payload)
      .subscribe(
        resposta => this.aplicarResposta(resposta),
        erro => this.tratarErro('Não foi possível analisar as oportunidades contra o currículo.', erro)
      );
  }

  private aplicarResposta(resposta: RankingOportunidadesCurriculoResponse) {
    this.carregando = false;
    this.curriculoReferencia = resposta.curriculoReferencia;
    this.totalOportunidades = resposta.totalOportunidades;
    var lista = resposta.oportunidades || [];
    this.oportunidades = lista.filter(item => item.scoreFinal > 0);
    this.oportunidadesIgnoradas = lista.filter(item => item.scoreFinal === 0);
  }

  private tratarErro(mensagem: string, erro: any) {
    console.error(mensagem, erro);
    this.erro = mensagem;
    this.carregando = false;
  }

  private getFiltroOportunidades() {
    return {
      where: {
        and: [
          { descricao: { neq: null } },
          { maisRecente: 1 }
        ]
      },
      order: 'data DESC',
      limit: 30
    };
  }

  private inferirNivel(item: OportunidadeLinkedin): string {
    var texto = ((item.titulo || '') + ' ' + (item.descricao || '')).toLowerCase();
    if (texto.indexOf('senior') >= 0 || texto.indexOf('sênior') >= 0) {
      return 'senior';
    }
    if (texto.indexOf('pleno') >= 0 || texto.indexOf('mid-level') >= 0) {
      return 'pleno';
    }
    if (texto.indexOf('junior') >= 0 || texto.indexOf('júnior') >= 0) {
      return 'junior';
    }
    return 'senior';
  }

  termosFormatados(termos: string[]): string {
    return termos && termos.length > 0 ? termos.join(', ') : '—';
  }
}
