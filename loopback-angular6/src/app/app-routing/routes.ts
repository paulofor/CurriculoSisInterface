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



export const routes : Routes = [
    { path: 'home' , component: HomeComponent },

    { path: 'palavraChave' , component: PalavraChaveListComponent},
    { path: 'palavraRaiz' , component: PalavraRaizListComponent },
    { path: 'palavraRaiz/:id' , component: PalavraRaizDetalheComponent },

    { path: 'palavraRaizQuantidade/:id' , component: PalavraRaizDetalheComQuantidadeComponent },

    { path: 'oportunidade/:id' , component: OportunidadeLinkedinDetalheComponent },
    { path: 'experienciaProfissional' , component: ExperienciaProfissionalLivreListComponent },
    { path: 'melhoresOportunidades' , component: MelhoresOportunidadesComponent },

    { path: 'oportunidadePalavra/:idPalavra/:idRaiz' , component: OportunidadeLinkedinListPorPalavraComponent },

    { path: '',  component: HomeComponent  }
    //{ path: '',          redirectTo: 'home', pathMatch: 'full' }
]
