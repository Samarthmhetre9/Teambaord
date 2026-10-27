import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { ApiService } from './api.service';
import { Project, STATUSES, STATUS_LABEL, Sprint, Status, Task, TaskEvent } from './models';
import { RealtimeService } from './realtime.service';
import { TaskDetailComponent } from './task-detail.component';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [DragDropModule, TaskDetailComponent],
  template: `
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h5 class="mb-0">{{ project.name }} <small class="text-muted">{{ project.key }}</small></h5>
      <button class="btn btn-accent btn-sm" (click)="creating = true">Add task</button>
    </div>

    <div class="row g-3" cdkDropListGroup>
      @for (s of statuses; track s) {
        <div class="col-12 col-md-6 col-xl-3">
          <div class="lane">
            <div class="lane-title"><span>{{ label[s] }}</span><span>{{ columns[s].length }}</span></div>
            <div class="drop-zone" cdkDropList [cdkDropListData]="columns[s]" (cdkDropListDropped)="drop($event, s)">
              @for (t of columns[s]; track t.id) {
                <div class="task-card" [class.high]="t.priority === 'HIGH'" [class.medium]="t.priority === 'MEDIUM'"
                     cdkDrag [cdkDragData]="t" (click)="selected = t">
                  <div class="d-flex justify-content-between mb-1">
                    <span class="type-tag" [class]="'type-tag ' + t.type">{{ t.type }}</span>
                    <span class="meta">{{ t.assignee || 'Unassigned' }}</span>
                  </div>
                  <div>{{ t.title }}</div>
                  @if (t.epicTitle) { <div class="meta mt-1">Epic: {{ t.epicTitle }}</div> }
                </div>
              }
            </div>
          </div>
        </div>
      }
    </div>

    @if (selected || creating) {
      <div class="backdrop" (click)="close()"></div>
      <app-task-detail [task]="selected" [projectId]="project.id" [sprints]="sprints" [epics]="epics" [user]="user"
                       (closed)="close()" (saved)="upsert([$event])" />
    }
  `,
})
export class BoardComponent implements OnChanges, OnDestroy {
  private api = inject(ApiService);
  private rt = inject(RealtimeService);

  @Input({ required: true }) project!: Project;
  @Input() sprints: Sprint[] = [];
  @Input() sprintFilter = 'ALL';
  @Input() user = '';

  statuses = STATUSES;
  label = STATUS_LABEL;
  tasks: Task[] = [];
  columns: Record<Status, Task[]> = { TODO: [], IN_PROGRESS: [], REVIEW: [], DONE: [] };
  selected: Task | null = null;
  creating = false;
  private sub?: Subscription;

  get epics() { return this.tasks.filter((t) => t.type === 'EPIC'); }

  ngOnChanges(c: SimpleChanges) {
    if (c['project']) this.load();
    else if (c['sprintFilter']) this.rebuild();
  }
  ngOnDestroy() { this.sub?.unsubscribe(); }

  private load() {
    this.sub?.unsubscribe();
    this.sub = this.rt.projectEvents(this.project.id).subscribe((e) => this.onEvent(e));
    this.api.tasks(this.project.id).subscribe((t) => { this.tasks = t; this.rebuild(); });
  }

  /** Every browser gets the same event; applying it twice is harmless because we upsert by id. */
  private onEvent(e: TaskEvent) {
    if (e.type === 'DELETED' && e.deletedId != null) {
      this.tasks = this.tasks.filter((t) => t.id !== e.deletedId);
      if (this.selected?.id === e.deletedId) this.selected = null;
      this.rebuild();
    } else {
      this.upsert(e.tasks);
    }
  }

  upsert(changed: Task[]) {
    const byId = new Map(this.tasks.map((t) => [t.id, t] as const));
    changed.forEach((t) => byId.set(t.id, t));
    this.tasks = [...byId.values()];
    this.rebuild();
  }

  private rebuild() {
    const f = this.sprintFilter;
    const visible = this.tasks.filter((t) => f === 'ALL' || (f === 'BACKLOG' ? !t.sprintId : t.sprintId === +f));
    for (const s of STATUSES) {
      this.columns[s] = visible.filter((t) => t.status === s).sort((a, b) => a.position - b.position);
    }
  }

  drop(event: CdkDragDrop<Task[]>, status: Status) {
    const t = event.item.data as Task;
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
    }
    t.status = status;
    // The server positions against the full column, so translate the index when a sprint filter hides cards.
    const full = this.tasks.filter((x) => x.status === status && x.id !== t.id).sort((a, b) => a.position - b.position);
    const next = event.container.data[event.currentIndex + 1];
    const idx = next ? full.findIndex((x) => x.id === next.id) : full.length;
    this.api.moveTask(t.id, status, idx < 0 ? full.length : idx).subscribe({ error: () => this.load() });
  }

  close() { this.selected = null; this.creating = false; }
}
