# Classroom Pulse - Real-Time Classroom Status Application

Classroom Pulse is a full-stack, responsive, real-time classroom coordination web application. It connects teachers and students during live problem-solving sessions without requiring permanent accounts or database overhead.

---

## 1. Project Overview

During live classroom lectures or workshops, teachers often pose conceptual questions or coding exercises. Traditional tools require submitting written answers or continuous polling. Classroom Pulse enables:
1. A teacher creates a room and receives a unique 6-character room code.
2. Students join from their mobile devices or laptops using the room code and their name.
3. The teacher broadcasts a question in real-time.
4. The question instantly renders on all connected student screens, setting their status to **PENDING**.
5. When a student solves or thinks through the question, they tap a large **COMPLETED** button.
6. The teacher dashboard dynamically updates the student's status light to glowing emerald green without refreshing the page.
7. The teacher can monitor total, completed, and pending counts alongside completion percentages, reset statuses, publish subsequent questions, or conclude the session.

---

## 2. Architecture

```
                       +---------------------------------------+
                       |           Angular 16 Frontend         |
                       | (TypeScript, RxJS, Native CSS, STOMP) |
                       +-------------------+-------------------+
                                           |
                    REST (HTTP / JSON)     |     STOMP over WebSocket
              Mutation & Initial Snapshot  |     Real-Time Broadcasts
                                           |
                                           v
                       +---------------------------------------+
                       |         Spring Boot 3.2.5 Backend     |
                       |       (Java 17, Spring Web, STOMP)    |
                       +-------------------+-------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |    In-Memory ConcurrentHashMap Store  |
                       |        (Thread-Safe Repository)       |
                       +---------------------------------------+
```

### Architectural Decisions
- **REST APIs**: Used for state mutations (creating room, joining room, publishing questions, marking completions, resetting, closing) and retrieving initial state snapshots.
- **WebSocket/STOMP (`/ws`)**: Used for push broadcasts from the server to connected clients (`/topic/classrooms/{roomCode}`).
- **In-Memory Thread-Safe Storage**: `ClassroomMemoryRepository` backed by `ConcurrentHashMap<String, ClassroomSession>`, using atomic counters and synchronizations so simultaneous student completions do not corrupt state.
- **No Database / No JPA**: Pure in-memory operational state for low-latency live coordination.

---

## 3. Teacher Workflow

1. Navigate to `/` (Landing page) and select **"I am a Teacher"** (navigates to `/teacher/create`).
2. Click **"Start New Classroom Session"**.
3. The backend generates:
   - A unique 6-character room code (omitting confusing characters: 0, O, 1, I).
   - A secure `teacherToken` stored in `sessionStorage` and sent in the `X-Teacher-Token` header for teacher-only operations.
4. On `/teacher/:roomCode`, view:
   - Live room code with one-click **Copy** button.
   - Connection indicator (Live / Reconnecting / Disconnected).
   - Question composer with a 500-character limit and live counter.
   - Live summary metrics (Total, Completed, Pending, Percentage, Progress Bar).
   - Student roster sorted by: Pending students first, Completed students second, then alphabetically.
5. Publish Question: broadcasts instantly to all student screens and resets all student statuses to **PENDING**.
6. When students click COMPLETED, their indicators immediately switch from amber to bright glowing green.
7. The teacher can click **"Reset Statuses"** to re-poll the current question, publish the next question, or click **"End Session"** (with confirmation dialog) to gracefully close the room.

---

## 4. Student Workflow

1. Navigate to `/` and select **"I am a Student"** or directly open `/student/join`.
2. Enter the 6-character room code and a display name (2–50 characters).
3. The server validates room availability and name uniqueness (case-insensitively), creates a UUID `studentId`, and returns the initial state.
4. `studentId` and `studentName` are saved to `sessionStorage` so refreshing the browser retains the student's session.
5. On `/student/:roomCode`:
   - If no question is active, a waiting screen is displayed.
   - As soon as the teacher publishes a question, it appears immediately via WebSocket.
   - The student has a prominent, touch-optimized **"I'M DONE ✓"** button.
   - Clicking the button marks completion idempotently.
   - Once completed, the button confirms completion and locks to prevent accidental duplicate submissions.
   - If the teacher broadcasts a new question, the student screen resets automatically to PENDING.
   - If the teacher closes the session, the student receives a `CLASSROOM_CLOSED` broadcast and actions are disabled.

---

## 5. Folder Structure

```
Teacher student sys/
├── backend/
│   ├── .mvn/
│   │   └── wrapper/
│   │       └── maven-wrapper.properties
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/classroom/
│   │   │   │   ├── ClassroomApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   ├── CorsConfig.java
│   │   │   │   │   └── WebSocketConfig.java
│   │   │   │   ├── controller/
│   │   │   │   │   └── ClassroomController.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── ClassroomStateResponse.java
│   │   │   │   │   ├── CompleteQuestionRequest.java
│   │   │   │   │   ├── CreateClassroomResponse.java
│   │   │   │   │   ├── ErrorResponse.java
│   │   │   │   │   ├── JoinStudentRequest.java
│   │   │   │   │   ├── JoinStudentResponse.java
│   │   │   │   │   ├── PublishQuestionRequest.java
│   │   │   │   │   ├── QuestionDto.java
│   │   │   │   │   ├── StudentDto.java
│   │   │   │   │   └── WebSocketEvent.java
│   │   │   │   ├── exception/
│   │   │   │   │   ├── ClassroomNotFoundException.java
│   │   │   │   │   ├── DuplicateStudentException.java
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── InvalidClassroomStateException.java
│   │   │   │   │   ├── StudentNotFoundException.java
│   │   │   │   │   └── UnauthorizedTeacherException.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── ClassroomEventType.java
│   │   │   │   │   ├── ClassroomSession.java
│   │   │   │   │   ├── CompletionStatus.java
│   │   │   │   │   ├── Question.java
│   │   │   │   │   └── StudentStatus.java
│   │   │   │   └── service/
│   │   │   │       └── ClassroomService.java
│   │   │   │   └── repository/
│   │   │   │       └── ClassroomMemoryRepository.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/java/com/example/classroom/
│   │       ├── controller/ClassroomControllerTest.java
│   │       └── service/ClassroomServiceTest.java
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/
│   │   │   │   ├── guards/
│   │   │   │   │   ├── student.guard.ts
│   │   │   │   │   └── teacher.guard.ts
│   │   │   │   ├── models/
│   │   │   │   │   ├── classroom.model.ts
│   │   │   │   │   ├── question.model.ts
│   │   │   │   │   ├── student-status.model.ts
│   │   │   │   │   └── websocket-event.model.ts
│   │   │   │   └── services/
│   │   │   │       ├── classroom-api.service.ts
│   │   │   │       ├── classroom-socket.service.ts
│   │   │   │       └── session-storage.service.ts
│   │   │   ├── pages/
│   │   │   │   ├── landing/
│   │   │   │   ├── not-found/
│   │   │   │   ├── student-join/
│   │   │   │   ├── student-room/
│   │   │   │   ├── teacher-create/
│   │   │   │   └── teacher-dashboard/
│   │   │   ├── shared/components/
│   │   │   │   ├── confirmation-dialog/
│   │   │   │   ├── connection-indicator/
│   │   │   │   ├── loading-spinner/
│   │   │   │   ├── room-code-display/
│   │   │   │   └── status-light/
│   │   │   ├── app-routing.module.ts
│   │   │   ├── app.component.ts
│   │   │   └── app.module.ts
│   │   ├── environments/
│   │   │   ├── environment.prod.ts
│   │   │   └── environment.ts
│   │   ├── styles.css
│   │   └── index.html
│   ├── angular.json
│   ├── package.json
│   ├── tsconfig.json
│   └── vercel.json
│
└── README.md
```

---

## 6. Prerequisites

- **Java JDK**: Version 17 or higher
- **Node.js**: Version 18.x or higher
- **npm**: Version 9.x or higher
- Maven (included via `mvnw.cmd` / `./mvnw`)

---

## 7. Installation and Run Commands

### Backend (Spring Boot)
Open a terminal in `backend/`:
```bash
cd backend

# Run with Maven Wrapper
./mvnw spring-boot:run

# Or build standalone JAR and run
./mvnw clean package -DskipTests
java -jar target/classroom-tracker-1.0.0.jar
```
The backend starts at `http://localhost:8080`.

### Frontend (Angular 16)
Open a terminal in `frontend/`:
```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```
The application opens at `http://localhost:4200`.

---

## 8. Environment Configuration

### Frontend
- **Development** (`src/environments/environment.ts`):
  ```typescript
  export const environment = {
    production: false,
    apiBaseUrl: 'http://localhost:8080/api',
    webSocketUrl: 'ws://localhost:8080/ws'
  };
  ```
- **Production** (`src/environments/environment.prod.ts`):
  Dynamically uses current host protocol (`wss://` / `ws://`) and origin without hardcoding localhost.

### Backend (`application.properties` / Environment Variables)
- `PORT`: Server port (default: `8080`).
- `ALLOWED_ORIGINS`: Comma-separated CORS allowed origins (default: `http://localhost:4200`).

Example running with environment overrides:
```bash
PORT=9000 ALLOWED_ORIGINS="https://my-app.vercel.app" java -jar target/classroom-tracker-1.0.0.jar
```

---

## 9. WebSocket / STOMP Design

- **STOMP Endpoint**: `/ws` (and SockJS fallback at `/ws-sockjs`)
- **Subscription Topic**: `/topic/classrooms/{roomCode}`
- **Event Envelope**:
  ```json
  {
    "type": "STUDENT_STATUS_CHANGED",
    "roomCode": "AB7KQ2",
    "timestamp": "2026-10-16T05:17:33.775Z",
    "payload": {
      "roomCode": "AB7KQ2",
      "active": true,
      "currentQuestion": {
        "questionId": "57db91ce-...",
        "questionText": "Explain the JVM architecture.",
        "sequenceNumber": 1,
        "publishedAt": "2026-10-16T05:17:33.349Z"
      },
      "students": [
        {
          "studentId": "870de06b-...",
          "studentName": "Ayush",
          "status": "COMPLETED",
          "connected": true,
          "joinedAt": "2026-10-16T05:17:32.869Z",
          "completedAt": "2026-10-16T05:17:33.775Z"
        }
      ],
      "totalStudents": 1,
      "completedCount": 1,
      "pendingCount": 0,
      "completionPercentage": 100.0,
      "createdAt": "2026-10-16T05:17:32.000Z"
    }
  }
  ```

### Event Types
| Event Type | Trigger |
| :--- | :--- |
| `STUDENT_JOINED` | Student enters classroom |
| `QUESTION_PUBLISHED` | Teacher broadcasts new question |
| `STUDENT_STATUS_CHANGED` | Student clicks COMPLETED |
| `CLASSROOM_RESET` | Teacher resets statuses to PENDING |
| `CLASSROOM_CLOSED` | Teacher ends the classroom session |
| `CLASSROOM_STATE_UPDATED` | Reconnection or state reconciliation |

---

## 10. REST API Reference

| Method | Endpoint | Headers | Description | Status Codes |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | - | Health check | 200 |
| `POST` | `/api/classrooms` | - | Create a classroom | 201 |
| `GET` | `/api/classrooms/{roomCode}` | - | Get current safe classroom state | 200, 404 |
| `POST` | `/api/classrooms/{roomCode}/students` | - | Join a student to room | 201, 400, 404, 409 |
| `POST` | `/api/classrooms/{roomCode}/questions` | `X-Teacher-Token` | Publish question | 200, 400, 401, 404, 409 |
| `POST` | `/api/classrooms/{roomCode}/students/{studentId}/complete` | - | Mark question completed | 200, 400, 404, 409 |
| `POST` | `/api/classrooms/{roomCode}/reset` | `X-Teacher-Token` | Reset statuses to PENDING | 200, 401, 404, 409 |
| `POST` | `/api/classrooms/{roomCode}/close` | `X-Teacher-Token` | Close classroom session | 200, 401, 404 |

---

## 11. Test Commands

### Backend Tests
Runs 19 unit, integration, and concurrency tests:
```bash
cd backend
./mvnw test
```

### Frontend Tests
Runs 17 unit tests in Chrome Headless:
```bash
cd frontend
npm test -- --watch=false --browsers=ChromeHeadless
```

### End-to-End WebSocket & REST Integration Test
```bash
cd frontend
node test-integration.js
```

---

## 12. Deployment

### Frontend (Vercel)
The Angular application includes `vercel.json` configured for SPA routing.
1. Build command: `npm run build`
2. Output directory: `dist/frontend`
3. Route rewrites:
   ```json
   {
     "version": 2,
     "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
   }
   ```
4. Set environment configuration or proxy headers as appropriate.

### Backend (Docker / Cloud Hosting)
The backend includes a multi-stage `Dockerfile`:
```bash
cd backend
docker build -t classroom-tracker .
docker run -p 8080:8080 -e ALLOWED_ORIGINS="https://your-frontend.vercel.app" classroom-tracker
```
Compatible with any container hosting provider (Render, Railway, Fly.io, Google Cloud Run, AWS ECS).

---

## 13. Current Limitations

- **In-Memory Storage Only**: Classroom state is stored in memory (`ConcurrentHashMap`). If the backend process restarts, all active classrooms are cleared.
- **Single Instance Backend**: The current version assumes a single Spring Boot server instance. Multiple instances without Redis will not share in-memory state or STOMP broadcasts.
- **No Permanent Accounts**: No user login, passwords, or persistent profiles.
- **No Attendance History**: Student rosters and logs are session-scoped and not saved after session closure.
- **No Stored Question Bank**: Questions are entered live and not saved to a question repository.
- **Lightweight Teacher Auth**: Teacher token is generated per session and validated via header; not enterprise-grade authentication.

---

## 14. Future Improvements

- **Redis Integration**: Redis Pub/Sub for multi-instance STOMP message brokering and Redis key-value storage for distributed room state.
- **PostgreSQL Persistence**: Store classroom history, questions, and student participation metrics.
- **Teacher Accounts & Authentication**: OAuth2 / JWT login with role-based access control.
- **Saved Rosters & CSV Import**: Allow teachers to pre-populate attendance rosters from CSV files.
- **Question Bank**: Save and reuse questions, sequence question decks in advance.
- **Configurable Countdown Timers**: Allow teachers to attach optional thinking timers to questions.
- **Student Removal & Moderation**: Enable teachers to kick disruptive or duplicate student names from the roster.
- **Analytics & Export**: Export session participation statistics to CSV/PDF after session completion.

