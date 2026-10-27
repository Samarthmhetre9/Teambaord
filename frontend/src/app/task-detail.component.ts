import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Subscription } from 'rxjs';
import { ApiService } from './api.service';
import { Attachment, Priority, Sprint, Task, TaskComment, TaskPayload, TaskType } from './models';
import { RealtimeService } from './realtime.service';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe],
  template: `
    <aside class="panel" aria-label="Task details">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="mb-0">{{ task ? 'Edit task' : 'New task' }}</h5>
        <button class="btn-close" aria-label="Close" (click)="closed.emit()"></button>
      </div>

      <form [formGroup]="form" (ngSubmit)="save()" class="vstack gap-2">
        <div>
          <label class="form-label small mb-0" for="title">Title</label>
          <input id="title" class="form-control" formControlName="title" />
          @if (form.controls.title.touched && form.controls.title.invalid) {
            <div class="text-danger small">Enter a title.</div>
          }
        </div>
        <div>
          <label class="form-label small mb-0" for="desc">Description</label>
          <textarea id="desc" rows="3" class="form-control" formControlName="description"></textarea>
        </div>
        <div class="row g-2">
          <div class="col-6">
            <label class="form-label small mb-0" for="type">Type</label>
            <select id="type" class="form-select" formControlName="type">
              @for (t of types; track t) { <option [value]="t">{{ t }}</option> }
            </select>
          </div>
          <div class="col-6">
            <label class="form-label small mb-0" for="prio">Priority</label>
            <select id="prio" class="form-select" formControlName="priority">
              @for (p of priorities; track p) { <option [value]="p">{{ p }}</option> }
            </select>
          </div>
          <div class="col-6">
            <label class="form-label small mb-0" for="sprint">Sprint</label>
            <select id="sprint" class="form-select" formControlName="sprintId">
              <option [ngValue]="null">Backlog</option>
              @for (s of sprints; track s.id) { <option [ngValue]="s.id">{{ s.name }}</option> }
            </select>
          </div>
          <div class="col-6">
            <label class="form-label small mb-0" for="epic">Epic</label>
            <select id="epic" class="form-select" formControlName="epicId">
              <option [ngValue]="null">None</option>
              @for (e of epicChoices; track e.id) { <option [ngValue]="e.id">{{ e.title }}</option> }
            </select>
          </div>
        </div>
        <div>
          <label class="form-label small mb-0" for="assignee">Assignee</label>
          <input id="assignee" class="form-control" formControlName="assignee" placeholder="username" />
        </div>
        <div class="d-flex gap-2 mt-2">
          <button class="btn btn-accent" type="submit" [disabled]="form.invalid || busy">{{ task ? 'Save changes' : 'Create task' }}</button>
          @if (task) { <button class="btn btn-outline-danger ms-auto" type="button" (click)="remove()">Delete task</button> }
        </div>
      </form>

      @if (task) {
        <hr />
        <h6>Attachments</h6>
        @for (a of attachments; track a.id) {
          <div><a href="#" (click)="download(a, $event)">{{ a.fileName }}</a> <span class="text-muted small">{{ (a.size / 1024).toFixed(1) }} KB</span></div>
        } @empty { <p class="text-muted small">No files yet.</p> }
        <input type="file" class="form-control form-control-sm mt-2" (change)="upload($event)" aria-label="Attach a file" />
        @if (uploadError) { <div class="text-danger small mt-1">{{ uploadError }}</div> }

        <hr />
        <h6>Comments</h6>
        @for (c of comments; track c.id) {
          <div class="comment"><strong>{{ c.author }}</strong> <span class="text-muted small">{{ c.createdAt | date: 'short' }}</span><br />{{ c.text }}</div>
        } @empty { <p class="text-muted small">No comments yet.</p> }
        <div class="input-group mt-2">
          <input class="form-control" [value]="draft" (input)="draft = $any($event.target).value" (keyup.enter)="send()" placeholder="Write a comment" aria-label="Comment" />
          <button class="btn btn-accent" type="button" (click)="send()">Post comment</button>
        </div>
      }
    </aside>
  `,
})
export class TaskDetailComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private rt = inject(RealtimeService);
  private fb = inject(FormBuilder);

  @Input() task: Task | null = null;
  @Input({ required: true }) projectId!: number;
  @Input() sprints: Sprint[] = [];
  @Input() epics: Task[] = [];
  @Input() user = '';
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<Task>();

  types: TaskType[] = ['TASK', 'STORY', 'BUG', 'EPIC'];
  priorities: Priority[] = ['LOW', 'MEDIUM', 'HIGH'];
  comments: TaskComment[] = [];
  attachments: Attachment[] = [];
  draft = '';
  busy = false;
  uploadError = '';
  private subs: Subscription[] = [];

  form = this.fb.group({
    title: ['', Validators.required],
    description: [''],
    type: this.fb.control<TaskType>('TASK'),
    priority: this.fb.control<Priority>('MEDIUM'),
    assignee: [''],
    sprintId: this.fb.control<number | null>(null),
    epicId: this.fb.control<number | null>(null),
  });

  get epicChoices() { return this.epics.filter((e) => e.id !== this.task?.id); }

  ngOnInit() {
    const t = this.task;
    if (!t) return;
    this.form.patchValue({
      title: t.title, description: t.description ?? '', type: t.type, priority: t.priority,
      assignee: t.assignee ?? '', sprintId: t.sprintId ?? null, epicId: t.epicId ?? null,
    });
    this.api.comments(t.id).subscribe((c) => (this.comments = c));
    this.api.attachments(t.id).subscribe((a) => (this.attachments = a));
    // live updates from teammates
    this.subs.push(
      this.rt.comments(t.id).subscribe((c) => {
        if (!this.comments.some((x) => x.id === c.id)) this.comments = [...this.comments, c];
      }),
      this.rt.attachments(t.id).subscribe((a) => {
        if (!this.attachments.some((x) => x.id === a.id)) this.attachments = [...this.attachments, a];
      }),
    );
  }
  ngOnDestroy() { this.subs.forEach((s) => s.unsubscribe()); }

  save() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const body: TaskPayload = {
      title: v.title ?? '', description: v.description || null, type: v.type ?? 'TASK', priority: v.priority ?? 'MEDIUM',
      assignee: v.assignee || null, sprintId: v.sprintId, epicId: v.epicId,
    };
    this.busy = true;
    const call = this.task ? this.api.updateTask(this.task.id, body) : this.api.createTask(this.projectId, body);
    call.subscribe({
      next: (t) => { this.saved.emit(t); this.busy = false; this.closed.emit(); },
      error: () => (this.busy = false),
    });
  }

  remove() {
    if (this.task && confirm('Delete this task for everyone?')) {
      this.api.deleteTask(this.task.id).subscribe(() => this.closed.emit());
    }
  }

  send() {
    const text = this.draft.trim();
    if (!text || !this.task) return;
    this.rt.sendComment(this.task.id, this.user || 'guest', text);   // goes over STOMP, comes back via the topic
    this.draft = '';
  }

  upload(ev: Event) {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.task) return;
    this.uploadError = '';
    this.api.upload(this.task.id, file).subscribe({
      next: (a) => { if (!this.attachments.some((x) => x.id === a.id)) this.attachments = [...this.attachments, a]; input.value = ''; },
      error: () => (this.uploadError = 'Upload failed. Files must be under 20 MB.'),
    });
  }

  download(a: Attachment, ev: Event) {
    ev.preventDefault();
    this.api.attachmentUrl(a.id).subscribe((r) => window.open(r.url, '_blank'));
  }
}
