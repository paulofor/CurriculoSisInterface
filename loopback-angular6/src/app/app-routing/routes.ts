import { Routes } from '@angular/router';



import { HomeComponent } from '../home/home.component';
import { PalavraChaveListComponent } from '../palavra-chave-list/palavra-chave-list.component';
import { PalavraRaizListComponent } from '../palavra-raiz-list/palavra-raiz-list.component';
import { PalavraRaizDetalheComponent } from '../palavra-raiz-detalhe/palavra-raiz-detalhe.component';
import { PalavraRaizDetalheComQuantidadeComponent } from '../palavra-raiz-detalhe-com-quantidade/palavra-raiz-detalhe-com-quantidade.component';
import { OportunidadeLinkedinDetalheComponent } from '../oportunidade-linkedin-detalhe/oportunidade-linkedin-detalhe.component';
import { ExperienciaProfissionalLivreListComponent } from '../experiencia-profissional-livre-list/experiencia-profissional-livre-list.component';
import { OportunidadeLinkedinListPorPalavraComponent } from '../oportunidade-linkedin-list-por-palavra/oportunidade-linkedin-list-por-palavra.component';
import { MelhoresOportunidadesComponent } from '../melhores-oportunidades/melhores-oportunidades.component';
import { CnaeListComponent } from '../cnae-list/cnae-list.component';
import { CnaeSubnichoListComponent } from '../cnae-subnicho-list/cnae-subnicho-list.component';
import { SubnichoPipelineComponent } from '../subnicho-pipeline/subnicho-pipeline.component';



export const routes : Routes = [
    { path: 'home' , component: HomeComponent },

    { path: 'palavraChave' , component: PalavraChaveListComponent},
    { path: 'palavraRaiz' , component: PalavraRaizListComponent },
    { path: 'palavraRaiz/:id' , component: PalavraRaizDetalheComponent },

    { path: 'palavraRaizQuantidade/:id' , component: PalavraRaizDetalheComQuantidadeComponent },

    { path: 'oportunidade/:id' , component: OportunidadeLinkedinDetalheComponent },
    { path: 'experienciaProfissional' , component: ExperienciaProfissionalLivreListComponent },
    { path: 'melhoresOportunidades' , component: MelhoresOportunidadesComponent },
    { path: 'cnaes' , component: CnaeListComponent },
    { path: 'cnaes/:cnae/subnichos' , component: CnaeSubnichoListComponent },
    { path: 'subnichos/:id/pipeline' , component: SubnichoPipelineComponent },

    { path: 'oportunidadePalavra/:idPalavra/:idRaiz' , component: OportunidadeLinkedinListPorPalavraComponent },

    { path: '',  component: HomeComponent  }
    //{ path: '',          redirectTo: 'home', pathMatch: 'full' }
]
