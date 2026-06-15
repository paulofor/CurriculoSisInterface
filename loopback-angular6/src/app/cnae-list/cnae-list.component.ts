import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CnaeItem, OprmFlowService } from '../oprm/oprm-flow.service';

@Component({
  selector: 'app-cnae-list',
  templateUrl: './cnae-list.component.html',
  styleUrls: ['./cnae-list.component.css']
})
export class CnaeListComponent implements OnInit {

  cnaes: CnaeItem[] = [];

  constructor(private service: OprmFlowService, private router: Router) { }

  ngOnInit() {
    this.service.getCnaes().subscribe((lista) => this.cnaes = lista);
  }

  abrirSubnichos(cnae: CnaeItem) {
    this.router.navigate(['/cnaes', cnae.codigo, 'subnichos']);
  }
}
