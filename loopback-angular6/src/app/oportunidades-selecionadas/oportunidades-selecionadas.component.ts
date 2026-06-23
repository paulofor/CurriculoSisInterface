import { Component, OnInit } from '@angular/core';
import { OportunidadeLinkedinApi, OportunidadeLinkedinInterface } from '../shared/sdk';

interface OportunidadeSelecionada extends OportunidadeLinkedinInterface {
  notaAderencia?: number;
  analiseAderenciaIa?: string;
  statusAderencia?: string;
  dataAvaliacaoAderencia?: Date;
  dataEnvio?: Date;
  atualizandoEnvio?: boolean;
  quantidadeDuplicadas?: number;
}

interface ResumoJanelaVagas {
  total: number;
  selecionadas: number;
  descartadas: number;
  pendentes: number;
}

@Component({
  selector: 'app-oportunidades-selecionadas',
  templateUrl: './oportunidades-selecionadas.component.html',
  styleUrls: ['./oportunidades-selecionadas.component.css']
})
export class OportunidadesSelecionadasComponent implements OnInit {
  carregando = false;
  erro = '';
  oportunidades: OportunidadeSelecionada[] = [];
  resumoJanela: ResumoJanelaVagas = {
    total: 0,
    selecionadas: 0,
    descartadas: 0,
    pendentes: 0
  };

  constructor(private oportunidadeLinkedinApi: OportunidadeLinkedinApi) { }

  ngOnInit() {
    this.carregarSelecionadas();
  }

  carregarSelecionadas() {
    this.carregando = true;
    this.erro = '';

    this.oportunidadeLinkedinApi.find(this.getFiltroSelecionadas())
      .subscribe(
        (oportunidades: OportunidadeSelecionada[]) => this.aplicarResposta(oportunidades),
        erro => this.tratarErro('Não foi possível carregar as oportunidades dos últimos 3 meses.', erro)
      );
  }

  alternarCurriculoEnviado(oportunidade: OportunidadeSelecionada) {
    if (!oportunidade || !oportunidade.id || oportunidade.atualizandoEnvio) {
      return;
    }

    const jaEnviado = this.isCurriculoEnviado(oportunidade);
    oportunidade.atualizandoEnvio = true;
    this.erro = '';

    const requisicao = jaEnviado
      ? this.oportunidadeLinkedinApi.patchAttributes(oportunidade.id, { dataEnvio: null })
      : this.oportunidadeLinkedinApi.RegistraEnvio(oportunidade.id);

    requisicao.subscribe(
      () => {
        oportunidade.dataEnvio = jaEnviado ? undefined : new Date();
        oportunidade.atualizandoEnvio = false;
      },
      erro => {
        oportunidade.atualizandoEnvio = false;
        this.tratarErro('Não foi possível atualizar a marcação de currículo enviado.', erro);
      }
    );
  }

  isCurriculoEnviado(oportunidade: OportunidadeSelecionada): boolean {
    return !!(oportunidade && oportunidade.dataEnvio);
  }

  getPercentual(valor: number): number {
    return this.resumoJanela.total > 0 ? Math.round((valor / this.resumoJanela.total) * 100) : 0;
  }

  getQuantidadeDuplicadas(oportunidade: OportunidadeSelecionada): number {
    return oportunidade && oportunidade.quantidadeDuplicadas ? oportunidade.quantidadeDuplicadas : 1;
  }

  private aplicarResposta(oportunidades: OportunidadeSelecionada[]) {
    const oportunidadesJanela = oportunidades || [];
    const selecionadas = oportunidadesJanela.filter(oportunidade => this.isSelecionada(oportunidade));

    this.resumoJanela = {
      total: oportunidadesJanela.length,
      selecionadas: selecionadas.length,
      descartadas: oportunidadesJanela.filter(oportunidade => this.isDescartada(oportunidade)).length,
      pendentes: oportunidadesJanela.filter(oportunidade => this.isPendenteAnalise(oportunidade)).length
    };
    this.marcarDuplicadasPorTexto(selecionadas);
    this.oportunidades = selecionadas;
    this.carregando = false;
  }


  private marcarDuplicadasPorTexto(oportunidades: OportunidadeSelecionada[]) {
    const totaisPorTexto: { [texto: string]: number } = {};

    oportunidades.forEach(oportunidade => {
      const textoNormalizado = this.getTextoNormalizado(oportunidade);
      if (textoNormalizado) {
        totaisPorTexto[textoNormalizado] = (totaisPorTexto[textoNormalizado] || 0) + 1;
      }
    });

    oportunidades.forEach(oportunidade => {
      const textoNormalizado = this.getTextoNormalizado(oportunidade);
      oportunidade.quantidadeDuplicadas = textoNormalizado ? totaisPorTexto[textoNormalizado] : 1;
    });
  }

  private getTextoNormalizado(oportunidade: OportunidadeSelecionada): string {
    return oportunidade && oportunidade.descricao
      ? oportunidade.descricao.replace(/\s+/g, ' ').trim().toLowerCase()
      : '';
  }

  private tratarErro(mensagem: string, erro: any) {
    console.error(mensagem, erro);
    this.erro = mensagem;
    this.carregando = false;
  }

  getClasseScore(score: number = 0): string {
    return score >= 85 ? 'score-alto' : 'score-medio';
  }

  private isSelecionada(oportunidade: OportunidadeSelecionada): boolean {
    return oportunidade.statusAderencia === 'avaliada'
      && Number(oportunidade.notaAderencia || 0) >= 70
      && !this.isModeloPresencialOuHibrido(oportunidade);
  }

  private isDescartada(oportunidade: OportunidadeSelecionada): boolean {
    return oportunidade.statusAderencia === 'avaliada' && !this.isSelecionada(oportunidade);
  }

  private isPendenteAnalise(oportunidade: OportunidadeSelecionada): boolean {
    return oportunidade.statusAderencia !== 'avaliada';
  }

  private isModeloPresencialOuHibrido(oportunidade: OportunidadeSelecionada): boolean {
    const texto = [
      oportunidade.modelo,
      oportunidade.analiseAderenciaIa,
      oportunidade.descricao
    ]
      .filter(valor => !!valor)
      .join(' ')
      .toLowerCase();

    return texto.indexOf('presencial') >= 0
      || texto.indexOf('híbrido') >= 0
      || texto.indexOf('hibrido') >= 0
      || texto.indexOf('hybrid') >= 0
      || texto.indexOf('on-site') >= 0
      || texto.indexOf('onsite') >= 0;
  }

  private getFiltroSelecionadas() {
    return {
      where: {
        data: { gte: this.getDataInicioJanela() }
      },
      order: ['notaAderencia DESC', 'data DESC'],
      limit: 1000
    };
  }

  private getDataInicioJanela(): Date {
    const data = new Date();
    data.setMonth(data.getMonth() - 3);
    data.setHours(0, 0, 0, 0);
    return data;
  }
}
