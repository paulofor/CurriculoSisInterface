import { Component, OnInit } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { OportunidadeLinkedinApi, OportunidadeLinkedinInterface } from '../shared/sdk';

interface OportunidadeSelecionada extends OportunidadeLinkedinInterface {
  notaAderencia?: number;
  analiseAderenciaIa?: string;
  statusAderencia?: string;
  dataAvaliacaoAderencia?: Date;
  dataEnvio?: Date;
  atualizandoEnvio?: boolean;
  quantidadeDuplicadas?: number;
  duplicadasMesmoTexto?: OportunidadeSelecionada[];
  sincronizarDataEnvio?: boolean;
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

    const duplicadas = this.getDuplicadasMesmoTexto(oportunidade);
    duplicadas.forEach(item => item.atualizandoEnvio = true);

    const dataEnvio = jaEnviado ? undefined : new Date();
    const requisicoes = duplicadas
      .filter(item => !!item.id)
      .map(item => jaEnviado
        ? this.oportunidadeLinkedinApi.patchAttributes(item.id, { dataEnvio: null })
        : this.oportunidadeLinkedinApi.RegistraEnvio(item.id));

    this.executarRequisicoes(requisicoes).subscribe(
      () => {
        duplicadas.forEach(item => {
          item.dataEnvio = dataEnvio;
          item.atualizandoEnvio = false;
        });
      },
      erro => {
        duplicadas.forEach(item => item.atualizandoEnvio = false);
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
    this.oportunidades = this.consolidarDuplicadasPorTexto(selecionadas);
    this.sincronizarEnvioDuplicadas(this.oportunidades);
    this.carregando = false;
  }

  private consolidarDuplicadasPorTexto(oportunidades: OportunidadeSelecionada[]): OportunidadeSelecionada[] {
    const gruposPorTexto: { [texto: string]: OportunidadeSelecionada[] } = {};
    const semTexto: OportunidadeSelecionada[] = [];

    oportunidades.forEach(oportunidade => {
      const textoNormalizado = this.getTextoNormalizado(oportunidade);
      if (!textoNormalizado) {
        semTexto.push(oportunidade);
        return;
      }
      gruposPorTexto[textoNormalizado] = gruposPorTexto[textoNormalizado] || [];
      gruposPorTexto[textoNormalizado].push(oportunidade);
    });

    const consolidadas = Object.keys(gruposPorTexto).map(texto => {
      const grupo = gruposPorTexto[texto].sort((a, b) => this.getTimestamp(b.data) - this.getTimestamp(a.data));
      const envio = grupo.find(item => this.isCurriculoEnviado(item));
      if (envio) {
        grupo.forEach(item => {
          item.sincronizarDataEnvio = !this.isCurriculoEnviado(item);
          item.dataEnvio = envio.dataEnvio;
        });
      }
      const maisRecente = grupo[0];
      maisRecente.quantidadeDuplicadas = grupo.length;
      maisRecente.duplicadasMesmoTexto = grupo;
      return maisRecente;
    });

    semTexto.forEach(oportunidade => {
      oportunidade.quantidadeDuplicadas = 1;
      oportunidade.duplicadasMesmoTexto = [oportunidade];
    });

    return consolidadas
      .concat(semTexto)
      .sort((a, b) => Number(b.notaAderencia || 0) - Number(a.notaAderencia || 0) || this.getTimestamp(b.data) - this.getTimestamp(a.data));
  }

  private sincronizarEnvioDuplicadas(oportunidades: OportunidadeSelecionada[]) {
    const requisicoes: Observable<any>[] = [];

    oportunidades.forEach(oportunidade => {
      const duplicadas = this.getDuplicadasMesmoTexto(oportunidade);
      const envio = duplicadas.find(item => this.isCurriculoEnviado(item));
      if (!envio || !envio.dataEnvio) {
        return;
      }
      duplicadas
        .filter(item => !!item.id && item.sincronizarDataEnvio)
        .forEach(item => requisicoes.push(this.oportunidadeLinkedinApi.patchAttributes(item.id, { dataEnvio: envio.dataEnvio })));
    });

    if (!requisicoes.length) {
      return;
    }

    this.executarRequisicoes(requisicoes).subscribe(
      () => undefined,
      erro => this.tratarErro('Não foi possível replicar a marcação de currículo enviado nas oportunidades duplicadas.', erro)
    );
  }


  private getDuplicadasMesmoTexto(oportunidade: OportunidadeSelecionada): OportunidadeSelecionada[] {
    return oportunidade && oportunidade.duplicadasMesmoTexto && oportunidade.duplicadasMesmoTexto.length
      ? oportunidade.duplicadasMesmoTexto
      : [oportunidade];
  }

  private executarRequisicoes(requisicoes: Observable<any>[]): Observable<any> {
    return requisicoes.length > 1 ? forkJoin(requisicoes) : requisicoes[0];
  }

  private getTimestamp(data: any): number {
    return data ? new Date(data).getTime() || 0 : 0;
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
