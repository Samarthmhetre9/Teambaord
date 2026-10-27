# TeamBoard: real-time project management (Jira/Trello-style)

Spring Boot + STOMP WebSockets + PostgreSQL on the backend, Angular + TypeScript on the frontend.

## 1. Install first
| Tool | Version | Check with |
|---|---|---|
| JDK | 17 or newer | `java -version` |
| Node.js | 20 LTS or newer (includes npm) | `node -v` |
| Docker Desktop | any recent | `docker -v` |
| Gradle | 8.x (only to create the wrapper once) | `gradle -v` |
| Git | optional | `git --version` |

VS Code extensions: Extension Pack for Java, Spring Boot Extension Pack, Gradle for Java, Angular Language Service, Docker.
(If you skip Docker, install PostgreSQL 16 yourself and create DB/user/password `teamboard`.)

## 2. Run (3 terminals in VS Code: Terminal > New Terminal)

**Terminal 1: database**
```
docker compose up -d db
```

**Terminal 2: backend** (http://localhost:8080)
```
cd backend
gradle wrapper        # one time only; creates gradlew
./gradlew bootRun     # Windows: gradlew.bat bootRun
```
First start creates tables and a demo project automatically.

**Terminal 3: frontend** (http://localhost:4200)
```
cd frontend
npm install
npm start
```

Open http://localhost:4200 in two browser windows. Drag a card, add a comment or edit a task in one window and watch the other update instantly.

## 3. Attachments on AWS S3 (optional)
Without setup, files are saved in `backend/uploads`. To use S3:
1. Create a private bucket. Create an IAM user with `s3:PutObject` and `s3:GetObject` on that bucket.
2. In the backend terminal set `S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, then run `./gradlew bootRun`.
3. Downloads then use presigned URLs that expire after 10 minutes.

## 4. Architecture
- `BoardController` / `TaskService`: REST for projects, sprints, tasks. `move()` renumbers the source and target columns, then broadcasts to `/topic/projects/{id}/tasks`.
- `CollabController`: comments over STOMP (`/app/tasks/{id}/comment` -> `/topic/tasks/{id}/comments`) and file attachments.
- `Task` is one table for EPIC / STORY / TASK / BUG; stories link to epics with a self-reference.
- Query tuning: composite index on `(project_id, status, position)`, `@EntityGraph` to avoid N+1 on sprint/epic, `default_batch_fetch_size`, DTOs so lazy entities never reach JSON, `open-in-view=false`.
- Frontend: `BoardComponent` (CDK drag-and-drop, upserts events by id so duplicates are harmless), `TaskDetailComponent` (typed reactive form, live comments, uploads), `RealtimeService` (RxStomp, auto-reconnect).

## 6. Known limits (be upfront about these)
No login: the "You are" box is just a display name. Sprints have no start/complete workflow yet. The in-memory STOMP broker supports a single backend instance.
