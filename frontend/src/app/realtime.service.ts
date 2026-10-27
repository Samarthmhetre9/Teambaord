import { Injectable } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { Observable, map } from 'rxjs';
import { Attachment, TaskComment, TaskEvent } from './models';

/** One shared STOMP-over-WebSocket connection. watch() can be called before the socket is open. */
@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private stomp = new RxStomp();

  constructor() {
    this.stomp.configure({ brokerURL: 'ws://localhost:8080/ws', reconnectDelay: 2000 });
    this.stomp.activate();
  }

  projectEvents(projectId: number): Observable<TaskEvent> {
    return this.stomp.watch(`/topic/projects/${projectId}/tasks`).pipe(map((m) => JSON.parse(m.body) as TaskEvent));
  }
  comments(taskId: number): Observable<TaskComment> {
    return this.stomp.watch(`/topic/tasks/${taskId}/comments`).pipe(map((m) => JSON.parse(m.body) as TaskComment));
  }
  attachments(taskId: number): Observable<Attachment> {
    return this.stomp.watch(`/topic/tasks/${taskId}/attachments`).pipe(map((m) => JSON.parse(m.body) as Attachment));
  }
  sendComment(taskId: number, author: string, text: string) {
    this.stomp.publish({ destination: `/app/tasks/${taskId}/comment`, body: JSON.stringify({ author, text }), headers: { 'content-type': 'application/json' } });
  }
}
