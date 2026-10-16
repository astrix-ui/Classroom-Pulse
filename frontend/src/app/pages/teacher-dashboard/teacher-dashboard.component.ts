import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { Subject, timer, of } from 'rxjs';
import { takeUntil, catchError } from 'rxjs/operators';
import { ClassroomApiService } from '../../core/services/classroom-api.service';
import { ClassroomSocketService } from '../../core/services/classroom-socket.service';
import { SessionStorageService } from '../../core/services/session-storage.service';
import { ClassroomSession, StudentGroupDto } from '../../core/models/classroom.model';
import { ConnectionState, WebSocketEvent } from '../../core/models/websocket-event.model';
import { StudentDto } from '../../core/models/student-status.model';
import { QuestionDto } from '../../core/models/question.model';
import { ThemeService } from '../../core/services/theme.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-teacher-dashboard',
  templateUrl: './teacher-dashboard.component.html',
  styleUrls: ['./teacher-dashboard.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TeacherDashboardComponent implements OnInit, OnDestroy {
  roomCode = '';
  teacherToken = '';

  classroom: ClassroomSession | null = null;
  connectionState: ConnectionState = 'CONNECTING';

  loading = true;
  submittingQuestion = false;
  resettingStatus = false;
  closingClassroom = false;
  showCloseConfirm = false;
  errorMessage = '';

  // Edit Question State
  isEditingQuestion = false;
  editQuestionText = '';
  savingEditQuestion = false;

  // Delete Question State
  showDeleteQuestionConfirm = false;
  deletingQuestion = false;

  // Session Ended State
  sessionEnded = false;

  // Allow Answers State
  togglingAllowAnswers = false;

  // Groups State
  configuringGroups = false;
  resettingGroups = false;
  groupSizeInput = 2;

  // Student Inspection Modal State
  selectedStudent: StudentDto | null = null;

  questionForm = new FormGroup({
    questionText: new FormControl('', [
      Validators.required,
      Validators.maxLength(500)
    ])
  });

  private readonly destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ClassroomApiService,
    private socketService: ClassroomSocketService,
    private sessionStorageService: SessionStorageService,
    public themeService: ThemeService,
    private toastService: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.roomCode = (this.route.snapshot.paramMap.get('roomCode') || '').toUpperCase();
    this.teacherToken = this.sessionStorageService.getTeacherToken(this.roomCode) || '';

    if (!this.teacherToken) {
      this.router.navigate(['/teacher/create'], { queryParams: { error: 'session_expired' } });
      return;
    }

    this.loadClassroom();
    this.initWebSocket();
    this.initKeepAlive();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.socketService.disconnect();
  }

  get questionTextControl(): FormControl {
    return this.questionForm.get('questionText') as FormControl;
  }

  get charCount(): number {
    return this.questionTextControl.value?.length || 0;
  }

  get hasActiveQuestion(): boolean {
    return !!this.classroom?.currentQuestion;
  }

  get isAllowAnswers(): boolean {
    return !!this.classroom?.allowAnswers;
  }

  get allQuestions(): QuestionDto[] {
    return this.classroom?.questions || [];
  }

  get isGroupsEnabled(): boolean {
    return !!this.classroom?.groupsEnabled;
  }

  get maxGroupSize(): number {
    return this.classroom?.maxGroupSize || 2;
  }

  get groups(): StudentGroupDto[] {
    return this.classroom?.groups || [];
  }

  get unassignedStudents(): StudentDto[] {
    return this.students.filter(s => !s.groupId);
  }

  get students(): StudentDto[] {
    return this.classroom?.students || [];
  }

  private loadClassroom(): void {
    this.loading = true;
    this.cdr.markForCheck();

    this.apiService.getClassroom(this.roomCode).subscribe({
      next: (data) => {
        this.classroom = data;
        if (data && !data.active) {
          this.sessionEnded = true;
        }
        if (this.selectedStudent && data?.students) {
          const updatedStudent = data.students.find(s => s.studentId === this.selectedStudent!.studentId);
          if (updatedStudent) {
            this.selectedStudent = updatedStudent;
          }
        }
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Failed to load classroom state.';
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
          this.classroom = event.payload;
          if (this.selectedStudent && this.classroom.students) {
            const updatedStudent = this.classroom.students.find(s => s.studentId === this.selectedStudent!.studentId);
            if (updatedStudent) {
              this.selectedStudent = updatedStudent;
            }
          }
          this.cdr.markForCheck();
        }
      });

    this.socketService.reconnected$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        // Resynchronize on reconnect
        this.apiService.getClassroom(this.roomCode).subscribe({
          next: (data) => {
            this.classroom = data;
            this.cdr.markForCheck();
          }
        });
      });
  }

  onPublishQuestion(): void {
    if (this.questionForm.invalid || this.submittingQuestion) {
      return;
    }

    const text = this.questionTextControl.value?.trim();
    if (!text) return;

    this.submittingQuestion = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.publishQuestion(this.roomCode, this.teacherToken, text).subscribe({
      next: () => {
        this.submittingQuestion = false;
        this.questionForm.reset({ questionText: '' });
        this.toastService.success('Question published to students');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.submittingQuestion = false;
        this.errorMessage = err?.error?.message || 'Failed to publish question.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onResetStatus(): void {
    if (this.resettingStatus || !this.classroom?.active) return;

    this.resettingStatus = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.resetClassroom(this.roomCode, this.teacherToken).subscribe({
      next: (updated) => {
        this.classroom = updated;
        this.resettingStatus = false;
        this.toastService.info('All student statuses reset to Pending');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.resettingStatus = false;
        this.errorMessage = err?.error?.message || 'Failed to reset statuses.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onStartEditQuestion(): void {
    if (!this.classroom?.currentQuestion) return;
    this.isEditingQuestion = true;
    this.editQuestionText = this.classroom.currentQuestion.questionText;
    this.cdr.markForCheck();
  }

  onCancelEditQuestion(): void {
    this.isEditingQuestion = false;
    this.editQuestionText = '';
    this.cdr.markForCheck();
  }

  onSaveEditQuestion(): void {
    const text = this.editQuestionText?.trim();
    if (!text || this.savingEditQuestion || !this.classroom?.active) return;

    this.savingEditQuestion = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.editQuestion(this.roomCode, this.teacherToken, text).subscribe({
      next: (updatedQuestion) => {
        if (this.classroom && this.classroom.currentQuestion) {
          this.classroom.currentQuestion = updatedQuestion;
        }
        this.isEditingQuestion = false;
        this.savingEditQuestion = false;
        this.toastService.info('Question updated');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.savingEditQuestion = false;
        this.errorMessage = err?.error?.message || 'Failed to update question.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  openDeleteQuestionDialog(): void {
    if (!this.classroom?.currentQuestion || !this.classroom?.active) return;
    this.showDeleteQuestionConfirm = true;
    this.cdr.markForCheck();
  }

  cancelDeleteQuestionDialog(): void {
    this.showDeleteQuestionConfirm = false;
    this.cdr.markForCheck();
  }

  confirmDeleteQuestion(): void {
    this.showDeleteQuestionConfirm = false;
    this.deletingQuestion = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.deleteQuestion(this.roomCode, this.teacherToken).subscribe({
      next: (updatedClassroom) => {
        this.classroom = updatedClassroom;
        this.isEditingQuestion = false;
        this.deletingQuestion = false;
        this.toastService.info('Question deleted');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.deletingQuestion = false;
        this.errorMessage = err?.error?.message || 'Failed to delete question.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  openCloseDialog(): void {
    this.showCloseConfirm = true;
    this.cdr.markForCheck();
  }

  cancelCloseDialog(): void {
    this.showCloseConfirm = false;
    this.cdr.markForCheck();
  }

  confirmCloseClassroom(): void {
    this.showCloseConfirm = false;
    this.closingClassroom = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.closeClassroom(this.roomCode, this.teacherToken).subscribe({
      next: (closedClassroom) => {
        this.closingClassroom = false;
        this.sessionEnded = true;
        if (this.classroom) {
          this.classroom.active = false;
        } else {
          this.classroom = closedClassroom;
        }
        this.toastService.warning('Classroom session ended');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.closingClassroom = false;
        this.errorMessage = err?.error?.message || 'Failed to close classroom.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onGoHome(): void {
    this.sessionStorageService.removeTeacherToken(this.roomCode);
    this.socketService.disconnect();
    this.router.navigate(['/']);
  }

  filterStatus: 'ALL' | 'COMPLETED' | 'PENDING' = 'ALL';
  filterGroup: string = 'ALL';
  searchQuery = '';
  managingGroupMember = false;

  get filteredStudents(): StudentDto[] {
    let list = this.students;
    if (this.filterStatus === 'COMPLETED') {
      list = list.filter(s => s.status === 'COMPLETED');
    } else if (this.filterStatus === 'PENDING') {
      list = list.filter(s => s.status === 'PENDING');
    }
    if (this.filterGroup === 'UNASSIGNED') {
      list = list.filter(s => !s.groupId);
    } else if (this.filterGroup && this.filterGroup !== 'ALL') {
      list = list.filter(s => s.groupId === this.filterGroup);
    }
    if (this.searchQuery && this.searchQuery.trim()) {
      const q = this.searchQuery.trim().toLowerCase();
      list = list.filter(s => s.studentName.toLowerCase().includes(q));
    }
    return list;
  }

  setFilter(status: 'ALL' | 'COMPLETED' | 'PENDING'): void {
    this.filterStatus = status;
  }

  setGroupFilter(group: string): void {
    this.filterGroup = group;
  }

  onToggleAllowAnswers(): void {
    if (this.togglingAllowAnswers || !this.classroom?.active) return;

    const nextState = !this.isAllowAnswers;
    this.togglingAllowAnswers = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.toggleAllowAnswers(this.roomCode, this.teacherToken, nextState).subscribe({
      next: (updated) => {
        this.classroom = updated;
        this.togglingAllowAnswers = false;
        this.toastService.info(this.isAllowAnswers ? 'Written answers enabled' : 'Simple mode enabled (answers disabled)');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.togglingAllowAnswers = false;
        this.errorMessage = err?.error?.message || 'Failed to update answer permissions.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onToggleGroups(): void {
    if (this.configuringGroups || !this.classroom?.active) return;

    const nextEnabled = !this.isGroupsEnabled;
    this.configuringGroups = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.configureGroups(this.roomCode, this.teacherToken, nextEnabled, this.groupSizeInput).subscribe({
      next: (updated) => {
        this.classroom = updated;
        this.configuringGroups = false;
        this.toastService.info(this.isGroupsEnabled ? `Group formation enabled (size ${this.maxGroupSize})` : 'Groups disabled');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.configuringGroups = false;
        this.errorMessage = err?.error?.message || 'Failed to update group settings.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onGroupSizeChange(newSize: number): void {
    if (newSize < 2 || newSize > 20) return;
    this.groupSizeInput = newSize;

    if (this.isGroupsEnabled && !this.configuringGroups && this.classroom?.active) {
      this.configuringGroups = true;
      this.errorMessage = '';
      this.cdr.markForCheck();

      this.apiService.configureGroups(this.roomCode, this.teacherToken, true, newSize).subscribe({
        next: (updated) => {
          this.classroom = updated;
          this.configuringGroups = false;
          this.toastService.info(`Group capacity updated to ${newSize}`);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.configuringGroups = false;
          this.errorMessage = err?.error?.message || 'Failed to update group size.';
          this.toastService.error(this.errorMessage);
          this.cdr.markForCheck();
        }
      });
    }
  }

  onResetGroups(): void {
    if (this.resettingGroups || !this.classroom?.active) return;

    this.resettingGroups = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.resetGroups(this.roomCode, this.teacherToken).subscribe({
      next: (updated) => {
        this.classroom = updated;
        this.resettingGroups = false;
        this.toastService.info('Groups have been reset');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.resettingGroups = false;
        this.errorMessage = err?.error?.message || 'Failed to reset groups.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onRemoveStudentFromGroup(groupId: string, studentId: string): void {
    if (this.managingGroupMember || !this.classroom?.active) return;

    this.managingGroupMember = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.removeStudentFromGroupByTeacher(this.roomCode, this.teacherToken, groupId, studentId).subscribe({
      next: (updated) => {
        this.classroom = updated;
        this.managingGroupMember = false;
        this.toastService.info('Student removed from group');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.managingGroupMember = false;
        this.errorMessage = err?.error?.message || 'Failed to remove student from group.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onAssignStudentToGroup(targetGroupId: string, studentId: string): void {
    if (!studentId || this.managingGroupMember || !this.classroom?.active) return;

    this.managingGroupMember = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.assignStudentToGroupByTeacher(this.roomCode, this.teacherToken, targetGroupId, studentId).subscribe({
      next: (updated) => {
        this.classroom = updated;
        this.managingGroupMember = false;
        this.toastService.success('Student assigned to group');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.managingGroupMember = false;
        this.errorMessage = err?.error?.message || 'Failed to assign student to group.';
        this.toastService.error(this.errorMessage);
        this.cdr.markForCheck();
      }
    });
  }

  onAddStudentToGroupFromSelect(targetGroupId: string, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const studentId = select.value;
    if (!studentId) return;

    this.onAssignStudentToGroup(targetGroupId, studentId);
    select.value = '';
  }

  getOtherGroupStudents(currentGroupId: string): StudentDto[] {
    return this.students.filter(s => s.groupId && s.groupId !== currentGroupId);
  }

  onSelectStudent(student: StudentDto): void {
    this.selectedStudent = student;
    this.cdr.markForCheck();
  }

  onCloseStudentModal(): void {
    this.selectedStudent = null;
    this.cdr.markForCheck();
  }

  isQuestionAnsweredByStudent(student: StudentDto | null, questionId: string): boolean {
    if (!student || !student.answeredQuestions) return false;
    return !!student.answeredQuestions[questionId];
  }

  getStudentAnsweredAt(student: StudentDto | null, questionId: string): string | null {
    if (!student || !student.answeredQuestions) return null;
    return student.answeredQuestions[questionId] || null;
  }

  getStudentAnswerText(student: StudentDto | null, questionId: string): string | null {
    if (!student || !student.answers) return null;
    return student.answers[questionId] || null;
  }

  getStudentAnsweredCount(student: StudentDto | null): number {
    if (!student || !student.answeredQuestions) return 0;
    return Object.keys(student.answeredQuestions).length;
  }

  getQuestionCompletionCount(questionId: string): number {
    if (!this.classroom || !this.classroom.students) return 0;
    return this.classroom.students.filter(s => s.answeredQuestions && s.answeredQuestions[questionId]).length;
  }

  trackByStudentId(index: number, student: StudentDto): string {
    return student.studentId;
  }

  trackByQuestionId(index: number, question: QuestionDto): string {
    return question.questionId;
  }

  /**
   * Periodically pings the backend every 60 seconds from the teacher role
   * to keep free-tier instances (e.g. Render) awake while students are writing answers.
   */
  private initKeepAlive(): void {
    timer(60000, 60000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.sendKeepAlivePing();
      });
  }

  sendKeepAlivePing(): void {
    if (!this.classroom?.active || this.sessionEnded) return;

    this.apiService.health()
      .pipe(
        catchError(() => of({ status: 'DOWN' }))
      )
      .subscribe();
  }
}
