import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ClassroomApiService } from '../../core/services/classroom-api.service';
import { ClassroomSocketService } from '../../core/services/classroom-socket.service';
import { SessionStorageService } from '../../core/services/session-storage.service';
import { ClassroomSession } from '../../core/models/classroom.model';
import { ConnectionState, WebSocketEvent } from '../../core/models/websocket-event.model';
import { CompletionStatus } from '../../core/models/student-status.model';

@Component({
  selector: 'app-student-room',
  templateUrl: './student-room.component.html',
  styleUrls: ['./student-room.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentRoomComponent implements OnInit, OnDestroy {
  roomCode = '';
  studentId = '';
  studentName = '';

  classroom: ClassroomSession | null = null;
  connectionState: ConnectionState = 'CONNECTING';

  submittingCompletion = false;
  inFlightQuestionId: string | null = null;
  errorMessage = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ClassroomApiService,
    private socketService: ClassroomSocketService,
    private sessionStorageService: SessionStorageService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.roomCode = (this.route.snapshot.paramMap.get('roomCode') || '').toUpperCase();
    const session = this.sessionStorageService.getStudentSession(this.roomCode);

    if (!session) {
      this.router.navigate(['/student/join'], {
        queryParams: { roomCode: this.roomCode, error: 'session_expired' }
      });
      return;
    }

    this.studentId = session.studentId;
    this.studentName = session.studentName;

    this.loadClassroom();
    this.initWebSocket();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.socketService.disconnect();
  }

  get currentQuestion() {
    return this.classroom?.currentQuestion || null;
  }

  get isClassroomActive(): boolean {
    return this.classroom?.active !== false;
  }

  get myStatus(): CompletionStatus {
    if (!this.classroom || !this.currentQuestion) return 'PENDING';
    const me = this.classroom.students.find(s => s.studentId === this.studentId);
    return me?.status || 'PENDING';
  }

  get isCompleted(): boolean {
    return this.myStatus === 'COMPLETED';
  }

  get canComplete(): boolean {
    return this.isClassroomActive &&
           !!this.currentQuestion &&
           !this.isCompleted &&
           !this.submittingCompletion;
  }

  private loadClassroom(): void {
    this.apiService.getClassroom(this.roomCode).subscribe({
      next: (data) => {
        this.classroom = data;
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (err.status === 404) {
          this.errorMessage = 'Classroom not found.';
        } else {
          this.errorMessage = 'Failed to load classroom state.';
        }
        this.cdr.markForCheck();
      }
    });
  }

  private initWebSocket(): void {
    this.socketService.connect(this.roomCode);

    this.socketService.connectionState$
      .pipe(takeUntil(this.destroy$))
      .subscribe((state) => {
        this.connectionState = state;
        this.cdr.markForCheck();
      });

    this.socketService.events$
      .pipe(takeUntil(this.destroy$))
      .subscribe((event: WebSocketEvent<ClassroomSession>) => {
        if (event && event.payload) {
          const prevQuestionId = this.classroom?.currentQuestion?.questionId;
          this.classroom = event.payload;

          // If a new question was published, discard in-flight completions for older questions
          if (this.classroom?.currentQuestion?.questionId !== prevQuestionId) {
            this.inFlightQuestionId = null;
            this.submittingCompletion = false;
          }

          this.cdr.markForCheck();
        }
      });

    this.socketService.reconnected$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.apiService.getClassroom(this.roomCode).subscribe({
          next: (data) => {
            this.classroom = data;
            this.cdr.markForCheck();
          }
        });
      });
  }

  onMarkCompleted(): void {
    if (!this.canComplete || !this.currentQuestion) return;

    const targetQuestionId = this.currentQuestion.questionId;
    this.inFlightQuestionId = targetQuestionId;
    this.submittingCompletion = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.completeQuestion(this.roomCode, this.studentId, targetQuestionId).subscribe({
      next: (updatedClassroom) => {
        // Ensure this completion corresponds to the current question
        if (this.currentQuestion?.questionId === targetQuestionId) {
          this.classroom = updatedClassroom;
        }
        this.submittingCompletion = false;
        this.inFlightQuestionId = null;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.submittingCompletion = false;
        this.inFlightQuestionId = null;
        this.errorMessage = err?.error?.message || 'Failed to update status. Please try again.';
        this.cdr.markForCheck();
      }
    });
  }

  showLeaveConfirm = false;
  leavingRoom = false;

  onLeaveRoom(): void {
    this.showLeaveConfirm = true;
    this.cdr.markForCheck();
  }

  cancelLeaveRoom(): void {
    this.showLeaveConfirm = false;
    this.cdr.markForCheck();
  }

  confirmLeaveRoom(): void {
    this.showLeaveConfirm = false;
    this.leavingRoom = true;
    this.cdr.markForCheck();

    this.apiService.leaveRoom(this.roomCode, this.studentId).subscribe({
      next: () => {
        this.socketService.disconnect();
        this.sessionStorageService.clearStudentSession(this.roomCode);
        this.router.navigate(['/']);
      },
      error: () => {
        this.socketService.disconnect();
        this.sessionStorageService.clearStudentSession(this.roomCode);
        this.router.navigate(['/']);
      }
    });
  }
}

