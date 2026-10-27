export type Status = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
export type TaskType = 'EPIC' | 'STORY' | 'TASK' | 'BUG';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

export const STATUSES: Status[] = ['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'];
export const STATUS_LABEL: Record<Status, string> = {
  TODO: 'To do', IN_PROGRESS: 'In progress', REVIEW: 'In review', DONE: 'Done',
};

export interface Project { id: number; name: string; key: string; }
export interface Sprint { id: number; name: string; goal?: string; projectId: number; }
export interface Task {
  id: number; title: string; description?: string | null; type: TaskType; status: Status; priority: Priority;
  position: number; assignee?: string | null; projectId: number;
  sprintId?: number | null; epicId?: number | null; epicTitle?: string | null;
}
export interface TaskPayload {
  title: string; description: string | null; type: TaskType; priority: Priority;
  assignee: string | null; sprintId: number | null; epicId: number | null;
}
export interface TaskEvent { type: 'CREATED' | 'UPDATED' | 'MOVED' | 'DELETED'; tasks: Task[]; deletedId?: number | null; }
export interface TaskComment { id: number; taskId: number; author: string; text: string; createdAt: string; }
export interface Attachment { id: number; taskId: number; fileName: string; contentType: string; size: number; }
