import { Component, OnInit } from '@angular/core';
import { OportunidadeLinkedinApi, OportunidadeLinkedinInterface } from '../shared/sdk';

interface OportunidadeSelecionada extends OportunidadeLinkedinInterface {
  notaAderencia?: number;
  analiseAderenciaIa?: string;
  statusAderencia?: string;
  dataAvaliacaoAderencia?: Date;
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
        erro => this.tratarErro('Não foi possível carregar as oportunidades selecionadas pelo avaliador de aderência.', erro)
      );
  }

  private aplicarResposta(oportunidades: OportunidadeSelecionada[]) {
    this.oportunidades = (oportunidades || [])
      .filter(oportunidade => !this.isModeloPresencialOuHibrido(oportunidade));
    this.carregando = false;
  }

  private tratarErro(mensagem: string, erro: any) {
    console.error(mensagem, erro);
    this.erro = mensagem;
    this.carregando = false;
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
        and: [
          { notaAderencia: { gte: 70 } },
          { statusAderencia: 'avaliada' },
          { modelo: { nin: ['Presencial', 'presencial', 'Híbrido', 'Hibrido', 'híbrido', 'hibrido'] } }
        ]
      },
      order: 'notaAderencia DESC',
      limit: 100
    };
  }
}
