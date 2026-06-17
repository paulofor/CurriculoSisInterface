/* tslint:disable */
import {
  PalavraRaiz,
  OportunidadePalavra
} from '../index';

declare var Object: any;
export interface OportunidadeLinkedinInterface {
  "descricao"?: string;
  "data"?: Date;
  "url"?: string;
  "tempo"?: string;
  "volume"?: string;
  "titulo"?: string;
  "palavraRaizId"?: number;
  "empresa"?: string;
  "maisRecente"?: number;
  "modelo"?: string;
  "dataEnvio"?: Date;
  "notaAderencia"?: number;
  "analiseAderenciaIa"?: string;
  "statusAderencia"?: string;
  "dataAvaliacaoAderencia"?: Date;
  "id"?: number;
  palavraRaiz?: PalavraRaiz;
  oportunidadePalavras?: OportunidadePalavra[];
}

export class OportunidadeLinkedin implements OportunidadeLinkedinInterface {
  "descricao": string;
  "data": Date;
  "url": string;
  "tempo": string;
  "volume": string;
  "titulo": string;
  "palavraRaizId": number;
  "empresa": string;
  "maisRecente": number;
  "modelo": string;
  "dataEnvio": Date;
  "notaAderencia": number;
  "analiseAderenciaIa": string;
  "statusAderencia": string;
  "dataAvaliacaoAderencia": Date;
  "id": number;
  palavraRaiz: PalavraRaiz;
  oportunidadePalavras: OportunidadePalavra[];
  constructor(data?: OportunidadeLinkedinInterface) {
    Object.assign(this, data);
  }
  /**
   * The name of the model represented by this $resource,
   * i.e. `OportunidadeLinkedin`.
   */
  public static getModelName() {
    return "OportunidadeLinkedin";
  }
  /**
  * @method factory
  * @author Jonathan Casarrubias
  * @license MIT
  * This method creates an instance of OportunidadeLinkedin for dynamic purposes.
  **/
  public static factory(data: OportunidadeLinkedinInterface): OportunidadeLinkedin{
    return new OportunidadeLinkedin(data);
  }
  /**
  * @method getModelDefinition
  * @author Julien Ledun
  * @license MIT
  * This method returns an object that represents some of the model
  * definitions.
  **/
  public static getModelDefinition() {
    return {
      name: 'OportunidadeLinkedin',
      plural: 'OportunidadeLinkedins',
      path: 'OportunidadeLinkedins',
      idName: 'id',
      properties: {
        "descricao": {
          name: 'descricao',
          type: 'string'
        },
        "data": {
          name: 'data',
          type: 'Date'
        },
        "url": {
          name: 'url',
          type: 'string'
        },
        "tempo": {
          name: 'tempo',
          type: 'string'
        },
        "volume": {
          name: 'volume',
          type: 'string'
        },
        "titulo": {
          name: 'titulo',
          type: 'string'
        },
        "palavraRaizId": {
          name: 'palavraRaizId',
          type: 'number'
        },
        "empresa": {
          name: 'empresa',
          type: 'string'
        },
        "maisRecente": {
          name: 'maisRecente',
          type: 'number'
        },
        "modelo": {
          name: 'modelo',
          type: 'string'
        },
        "dataEnvio": {
          name: 'dataEnvio',
          type: 'Date'
        },
        "notaAderencia": {
          name: 'notaAderencia',
          type: 'number'
        },
        "analiseAderenciaIa": {
          name: 'analiseAderenciaIa',
          type: 'string'
        },
        "statusAderencia": {
          name: 'statusAderencia',
          type: 'string'
        },
        "dataAvaliacaoAderencia": {
          name: 'dataAvaliacaoAderencia',
          type: 'Date'
        },
        "id": {
          name: 'id',
          type: 'number'
        },
      },
      relations: {
        palavraRaiz: {
          name: 'palavraRaiz',
          type: 'PalavraRaiz',
          model: 'PalavraRaiz',
          relationType: 'belongsTo',
                  keyFrom: 'palavraRaizId',
          keyTo: 'id'
        },
        oportunidadePalavras: {
          name: 'oportunidadePalavras',
          type: 'OportunidadePalavra[]',
          model: 'OportunidadePalavra',
          relationType: 'hasMany',
                  keyFrom: 'id',
          keyTo: 'oportunidadeLinkedinId'
        },
      }
    }
  }
}
