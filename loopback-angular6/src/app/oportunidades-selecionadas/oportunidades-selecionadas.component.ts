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
    this.oportunidades = oportunidades || [];
    this.carregando = false;
  }

  private tratarErro(mensagem: string, erro: any) {
    console.error(mensagem, erro);
    this.erro = mensagem;
    this.carregando = false;
  }

  private getFiltroSelecionadas() {
    return {
      where: {
        and: [
          { notaAderencia: { gte: 70 } },
          { statusAderencia: 'avaliada' }
        ]
      },
      order: 'notaAderencia DESC',
      limit: 100
    };
  }
}
