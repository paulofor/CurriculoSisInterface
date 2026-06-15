import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

export interface CnaeItem {
  codigo: string;
  descricao: string;
  totalSubnichos: number;
  custoTotal: number;
}

export interface SubnichoItem {
  id: number;
  cnae: string;
  nome: string;
  status: string;
  jobs: number;
  custoTotal: number;
  custoPorExecucao: number;
}

export interface PipelineEtapa {
  ordem: number;
  nome: string;
  descricao: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class OprmFlowService {

  private cnaes: CnaeItem[] = [
    { codigo: '9602501', descricao: 'Cabeleireiros, manicure e pedicure', totalSubnichos: 2, custoTotal: 1.4275 },
    { codigo: '8599604', descricao: 'Treinamento em desenvolvimento profissional e gerencial', totalSubnichos: 1, custoTotal: 0.3841 },
    { codigo: '7319002', descricao: 'Promoção de vendas', totalSubnichos: 1, custoTotal: 0.5120 }
  ];

  private subnichos: SubnichoItem[] = [
    {
      id: 960250101,
      cnae: '9602501',
      nome: 'Manicure autônoma que atende em domicílio e precisa manter clientes recorrentes',
      status: 'Rotina sintetizada',
      jobs: 7,
      custoTotal: 0.1892,
      custoPorExecucao: 0.0270
    },
    {
      id: 960250102,
      cnae: '9602501',
      nome: 'Barbeiro de bairro que vende recorrência para corte e barba mensal',
      status: 'Materialização pronta',
      jobs: 9,
      custoTotal: 0.2530,
      custoPorExecucao: 0.0281
    },
    {
      id: 859960401,
      cnae: '8599604',
      nome: 'Mentor técnico que vende turmas pequenas para transição de carreira',
      status: 'Sinais coletados',
      jobs: 5,
      custoTotal: 0.1418,
      custoPorExecucao: 0.0284
    },
    {
      id: 731900201,
      cnae: '7319002',
      nome: 'Promotor local que capta clientes por WhatsApp para comércio de bairro',
      status: 'Seed criada',
      jobs: 3,
      custoTotal: 0.0810,
      custoPorExecucao: 0.0270
    }
  ];

  getCnaes(): Observable<CnaeItem[]> {
    return of(this.cnaes);
  }

  getSubnichosPorCnae(cnae: string): Observable<SubnichoItem[]> {
    return of(this.subnichos.filter((item) => item.cnae === cnae));
  }

  getSubnicho(id: number): Observable<SubnichoItem> {
    return of(this.subnichos.find((item) => item.id === id));
  }

  criarNovoSubnicho(cnae: string): Observable<SubnichoItem> {
    const novo: SubnichoItem = {
      id: new Date().getTime(),
      cnae: cnae,
      nome: 'Novo subnicho com potencial de venda gerado a partir do CNAE e dos subnichos existentes',
      status: 'Nicho iniciado',
      jobs: 1,
      custoTotal: 0.0364,
      custoPorExecucao: 0.0364
    };
    this.subnichos.push(novo);

    const cnaeItem = this.cnaes.find((item) => item.codigo === cnae);
    if (cnaeItem) {
      cnaeItem.totalSubnichos = cnaeItem.totalSubnichos + 1;
      cnaeItem.custoTotal = cnaeItem.custoTotal + novo.custoTotal;
    }

    return of(novo);
  }

  getEtapasPipeline(): PipelineEtapa[] {
    return [
      { ordem: 1, nome: 'Nicho', descricao: 'Define o subnicho operacional e sua hipótese de venda.', status: 'Concluído' },
      { ordem: 2, nome: 'Histórico', descricao: 'Lista somente os jobs executados para o novo subnicho.', status: 'Em execução' },
      { ordem: 3, nome: 'Sinais', descricao: 'Extrai dores, rotina, linguagem e objeções do público.', status: 'Na fila' },
      { ordem: 4, nome: 'Síntese', descricao: 'Consolida evidências e gera recomendação de negócio.', status: 'Na fila' },
      { ordem: 5, nome: 'Materialização', descricao: 'Prepara o subnicho para ofertas e experimentos.', status: 'Na fila' }
    ];
  }
}
