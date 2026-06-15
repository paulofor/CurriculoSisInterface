import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OprmFlowService, SubnichoItem } from '../oprm/oprm-flow.service';

@Component({
  selector: 'app-cnae-subnicho-list',
  templateUrl: './cnae-subnicho-list.component.html',
  styleUrls: ['./cnae-subnicho-list.component.css']
})
export class CnaeSubnichoListComponent implements OnInit {

  cnae: string;
  subnichos: SubnichoItem[] = [];
  criando = false;

  constructor(private route: ActivatedRoute, private router: Router, private service: OprmFlowService) { }

  ngOnInit() {
    this.cnae = this.route.snapshot.paramMap.get('cnae');
    this.carregarSubnichos();
  }

  carregarSubnichos() {
    this.service.getSubnichosPorCnae(this.cnae).subscribe((lista) => this.subnichos = lista);
  }

  abrirSubnicho(subnicho: SubnichoItem) {
    this.router.navigate(['/subnichos', subnicho.id, 'pipeline']);
  }

  criarNovoSubnicho() {
    this.criando = true;
    this.service.criarNovoSubnicho(this.cnae).subscribe((novo) => {
      this.criando = false;
      this.router.navigate(['/subnichos', novo.id, 'pipeline']);
    });
  }
}
