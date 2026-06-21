import { Component } from '@angular/core';
import { MatDialog } from '@angular/material';
import { ActivatedRoute } from '@angular/router';
import { ExperienciaProfissionalLivre, ExperienciaProfissionalLivreApi } from '../shared/sdk';
import { ExperienciaProfissionalLivreListBaseComponent } from './experiencia-profissional-livre-list-base.component';

@Component({
  selector: 'app-experiencia-profissional-livre-list',
  templateUrl: './experiencia-profissional-livre-list.component.html',
  styleUrls: ['./experiencia-profissional-livre-list.component.css']
})
export class ExperienciaProfissionalLivreListComponent extends ExperienciaProfissionalLivreListBaseComponent {

  private termosTecnicos = [
    'java', 'spring', 'spring boot', 'python', 'spark', 'hadoop', 'databricks', 'jupyter', 'machine learning',
    'etl', 'rest', 'api', 'apis', 'microservices', 'microsserviços', 'sql', 'aws', 'cloud', 'docker',
    'kubernetes', 'jenkins', 'git', 'angular', 'javascript', 'typescript', 'ajax', 'mvc', 'jboss', 'html', 'css'
  ];
  resumosScore: any = {};

  constructor(protected srv: ExperienciaProfissionalLivreApi, protected router: ActivatedRoute, protected dialog: MatDialog) {
    super(srv, router, dialog);
  }

  getFiltro(): {} {
    return {
      'order' : 'dataInicio desc'
    };
  }

  getTagsTecnologia(item: ExperienciaProfissionalLivre): string[] {
    var texto = this.normalizarTexto([
      item.principaisTecnologias,
      item.descricaoLivre,
      item.descricaoGupy,
      item.tituloFuncao
    ].join(' '));

    return this.termosTecnicos
      .filter(termo => texto.indexOf(this.normalizarTexto(termo)) >= 0)
      .slice(0, 20);
  }

  isUsadoNoScore(item: ExperienciaProfissionalLivre): boolean {
    return !!(item && (item.descricaoLivre || item.principaisTecnologias || item.descricaoGupy));
  }

  gerarResumoParaScore(item: ExperienciaProfissionalLivre) {
    if (!item) {
      return;
    }

    this.resumosScore[item.id || 'novo'] = [
      this.montarCabecalho(item),
      item.descricaoLivre,
      item.principaisTecnologias ? 'Tecnologias: ' + item.principaisTecnologias : '',
      item.descricaoGupy ? 'Resumo Gupy: ' + item.descricaoGupy : ''
    ]
      .filter(valor => !!valor)
      .join(' ');
  }

  getResumoScore(item: ExperienciaProfissionalLivre): string {
    return item ? this.resumosScore[item.id || 'novo'] : '';
  }

  getTextoStatusScore(item: ExperienciaProfissionalLivre): string {
    return this.isUsadoNoScore(item) ? 'Usado no score' : 'Completar relato para entrar no score';
  }

  private montarCabecalho(item: ExperienciaProfissionalLivre): string {
    return [item.cliente, item.consultoria, item.tituloFuncao]
      .filter(valor => !!valor)
      .join(' - ');
  }

  private normalizarTexto(texto: string): string {
    return (texto || '')
      .toLowerCase()
      .replace(/[áàâã]/g, 'a')
      .replace(/[éê]/g, 'e')
      .replace(/[í]/g, 'i')
      .replace(/[óôõ]/g, 'o')
      .replace(/[ú]/g, 'u')
      .replace(/[ç]/g, 'c');
  }
}
