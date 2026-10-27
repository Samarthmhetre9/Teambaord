import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Attachment, Project, Sprint, Status, Task, TaskComment, TaskPayload } from './models';

export const API_HOST = 'http://localhost:8080';
const API = `${API_HOST}/api`;

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  projects() { return this.http.get<Project[]>(`${API}/projects`); }
  createProject(name: string, key: string) { return this.http.post<Project>(`${API}/projects`, { name, key }); }
  sprints(projectId: number) { return this.http.get<Sprint[]>(`${API}/projects/${projectId}/sprints`); }
  createSprint(projectId: number, name: string, goal: string) {
    return this.http.post<Sprint>(`${API}/projects/${projectId}/sprints`, { name, goal });
  }
  tasks(projectId: number) { return this.http.get<Task[]>(`${API}/projects/${projectId}/tasks`); }
  createTask(projectId: number, body: TaskPayload) { return this.http.post<Task>(`${API}/projects/${projectId}/tasks`, body); }
  updateTask(id: number, body: TaskPayload) { return this.http.put<Task>(`${API}/tasks/${id}`, body); }
  moveTask(id: number, status: Status, position: number) {
    return this.http.patch<Task[]>(`${API}/tasks/${id}/move`, { status, position });
  }
  deleteTask(id: number) { return this.http.delete<void>(`${API}/tasks/${id}`); }
  comments(taskId: number) { return this.http.get<TaskComment[]>(`${API}/tasks/${taskId}/comments`); }
  attachments(taskId: number) { return this.http.get<Attachment[]>(`${API}/tasks/${taskId}/attachments`); }
  upload(taskId: number, file: File) {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Attachment>(`${API}/tasks/${taskId}/attachments`, form);
  }
  attachmentUrl(id: number) { return this.http.get<{ url: string }>(`${API}/attachments/${id}/url`); }
}
