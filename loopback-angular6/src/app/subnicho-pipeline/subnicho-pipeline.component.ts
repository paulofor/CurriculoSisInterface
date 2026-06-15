import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OprmFlowService, PipelineEtapa, SubnichoItem } from '../oprm/oprm-flow.service';

@Component({
  selector: 'app-subnicho-pipeline',
  templateUrl: './subnicho-pipeline.component.html',
  styleUrls: ['./subnicho-pipeline.component.css']
})
export class SubnichoPipelineComponent implements OnInit {

  subnicho: SubnichoItem;
  etapas: PipelineEtapa[] = [];

  constructor(private route: ActivatedRoute, private service: OprmFlowService) { }

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.service.getSubnicho(id).subscribe((item) => this.subnicho = item);
    this.etapas = this.service.getEtapasPipeline();
  }
}
