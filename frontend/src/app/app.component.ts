import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from './api.service';
import { BoardComponent } from './board.component';
import { Project, Sprint } from './models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, BoardComponent],
  template: `
    <nav class="topbar py-2 px-3 d-flex flex-wrap gap-2 align-items-center">
      <span class="brand me-3">TeamBoard</span>
      <select class="form-select form-select-sm w-auto" [ngModel]="project?.id" (ngModelChange)="selectProject($event)">
        @for (p of projects; track p.id) { <option [ngValue]="p.id">{{ p.name }}</option> }
      </select>
      <button class="btn btn-sm btn-outline-light" (click)="newProject()">New project</button>
      <select class="form-select form-select-sm w-auto" [(ngModel)]="sprintFilter">
        <option value="ALL">All work</option>
        <option value="BACKLOG">Backlog (no sprint)</option>
        @for (s of sprints; track s.id) { <option [value]="s.id">{{ s.name }}</option> }
      </select>
      <button class="btn btn-sm btn-outline-light" (click)="newSprint()">New sprint</button>
      <span class="ms-auto d-flex align-items-center gap-2">
        <label class="small mb-0" for="who">You are</label>
        <input id="who" class="form-control form-control-sm" style="width:130px" [(ngModel)]="user" (ngModelChange)="saveUser()" />
      </span>
    </nav>
    <main class="container-fluid p-3">
      @if (project) {
        <app-board [project]="project" [sprints]="sprints" [sprintFilter]="sprintFilter" [user]="user" />
      } @else {
        <p class="text-muted">No project yet. Create one with “New project”.</p>
      }
    </main>
  `,
})
export class AppComponent implements OnInit {
  private api = inject(ApiService);
  projects: Project[] = [];
  project: Project | null = null;
  sprints: Sprint[] = [];
  sprintFilter = 'ALL';
  user = localStorage.getItem('tb-user') || 'guest-' + Math.floor(Math.random() * 900 + 100);

  ngOnInit() {
    this.saveUser();
    this.api.projects().subscribe((ps) => {
      this.projects = ps;
      if (ps.length) this.selectProject(ps[0].id);
    });
  }
  saveUser() { localStorage.setItem('tb-user', this.user); }

  selectProject(id: number) {
    this.project = this.projects.find((p) => p.id === id) ?? null;
    this.sprintFilter = 'ALL';
    this.sprints = [];
    if (this.project) this.api.sprints(this.project.id).subscribe((s) => (this.sprints = s));
  }
  newProject() {
    const name = prompt('Project name');
    const key = name ? prompt('Short key (e.g. WEB)') : null;
    if (!name || !key) return;
    this.api.createProject(name, key).subscribe((p) => { this.projects = [...this.projects, p]; this.selectProject(p.id); });
  }
  newSprint() {
    if (!this.project) return;
    const name = prompt('Sprint name');
    if (!name) return;
    const goal = prompt('Sprint goal (optional)') ?? '';
    this.api.createSprint(this.project.id, name, goal).subscribe((s) => (this.sprints = [s, ...this.sprints]));
  }
}
