import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, Subject } from 'rxjs';
import { TeacherDashboardComponent } from './teacher-dashboard.component';
import { ClassroomApiService } from '../../core/services/classroom-api.service';
import { ClassroomSocketService } from '../../core/services/classroom-socket.service';
import { SessionStorageService } from '../../core/services/session-storage.service';
import { ConnectionIndicatorComponent } from '../../shared/components/connection-indicator/connection-indicator.component';
import { RoomCodeDisplayComponent } from '../../shared/components/room-code-display/room-code-display.component';
import { StatusLightComponent } from '../../shared/components/status-light/status-light.component';
import { ConfirmationDialogComponent } from '../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ClassroomSession } from '../../core/models/classroom.model';
import { WebSocketEvent } from '../../core/models/websocket-event.model';

describe('TeacherDashboardComponent', () => {
  let component: TeacherDashboardComponent;
  let fixture: ComponentFixture<TeacherDashboardComponent>;
  let socketEventsSubject: Subject<WebSocketEvent<ClassroomSession>>;
  let apiService: jasmine.SpyObj<ClassroomApiService>;
  let sessionStorageService: jasmine.SpyObj<SessionStorageService>;

  const initialClassroom: ClassroomSession = {
    roomCode: 'AB7KQ2',
    active: true,
    currentQuestion: null,
    students: [],
    totalStudents: 0,
    completedCount: 0,
    pendingCount: 0,
    completionPercentage: 0,
    createdAt: new Date().toISOString()
  };

  beforeEach(async () => {
    socketEventsSubject = new Subject<WebSocketEvent<ClassroomSession>>();

    apiService = jasmine.createSpyObj('ClassroomApiService', [
      'getClassroom',
      'publishQuestion',
      'editQuestion',
      'deleteQuestion',
      'resetClassroom',
      'closeClassroom',
      'toggleAllowAnswers',
      'health'
    ]);
    apiService.getClassroom.and.returnValue(of(initialClassroom));
    apiService.deleteQuestion.and.returnValue(of(initialClassroom));
    apiService.health.and.returnValue(of({ status: 'UP' }));
    apiService.toggleAllowAnswers.and.returnValue(of({
      ...initialClassroom,
      allowAnswers: true
    }));

    const sessionSpy = jasmine.createSpyObj('SessionStorageService', ['getTeacherToken', 'removeTeacherToken']);
    sessionStorageService = sessionSpy;
    sessionSpy.getTeacherToken.and.returnValue('valid-teacher-token');

    const socketSpy = jasmine.createSpyObj('ClassroomSocketService', ['connect', 'disconnect'], {
      connectionState$: of('CONNECTED'),
      events$: socketEventsSubject.asObservable(),
      reconnected$: new Subject<void>().asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, FormsModule, RouterTestingModule, HttpClientTestingModule],
      declarations: [
        TeacherDashboardComponent,
        ConnectionIndicatorComponent,
        RoomCodeDisplayComponent,
        StatusLightComponent,
        ConfirmationDialogComponent
      ],
      providers: [
        { provide: ClassroomApiService, useValue: apiService },
        { provide: SessionStorageService, useValue: sessionSpy },
        { provide: ClassroomSocketService, useValue: socketSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => key === 'roomCode' ? 'AB7KQ2' : null
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TeacherDashboardComponent);
    component = fixture.componentInstance;
    component.roomCode = 'AB7KQ2';
    component.teacherToken = 'valid-teacher-token';
    fixture.detectChanges();
  });

  it('should validate publish question form', () => {
    expect(component.questionForm.valid).toBeFalse();

    // Valid question
    component.questionForm.patchValue({ questionText: 'What is polymorphism?' });
    expect(component.questionForm.valid).toBeTrue();

    // Over 500 chars
    component.questionForm.patchValue({ questionText: 'A'.repeat(501) });
    expect(component.questionForm.valid).toBeFalse();
  });

  it('should update teacher list when a WebSocket event is received', () => {
    const updatedClassroom: ClassroomSession = {
      ...initialClassroom,
      students: [
        {
          studentId: 's1',
          studentName: 'Alice',
          status: 'PENDING',
          connected: true,
          joinedAt: new Date().toISOString(),
          completedAt: null
        }
      ],
      totalStudents: 1,
      pendingCount: 1,
      completedCount: 0,
      completionPercentage: 0
    };

    socketEventsSubject.next({
      type: 'STUDENT_JOINED',
      roomCode: 'AB7KQ2',
      timestamp: new Date().toISOString(),
      payload: updatedClassroom
    });

    expect(component.classroom?.students.length).toBe(1);
    expect(component.classroom?.students[0].studentName).toBe('Alice');
    expect(component.students.length).toBe(1);
  });

  it('should toggle allow answers when requested', () => {
    expect(component.isAllowAnswers).toBeFalse();
    component.onToggleAllowAnswers();
    expect(apiService.toggleAllowAnswers).toHaveBeenCalledWith('AB7KQ2', 'valid-teacher-token', true);
    expect(component.classroom?.allowAnswers).toBeTrue();
  });

  it('should select student for inspection and calculate answers correctly', () => {
    const studentWithAnswers = {
      studentId: 's1',
      studentName: 'Alice',
      status: 'COMPLETED' as const,
      connected: true,
      joinedAt: new Date().toISOString(),
      completedAt: new Date().toISOString(),
      answeredQuestions: {
        'q1': new Date().toISOString(),
        'q2': new Date().toISOString()
      },
      answers: {
        'q1': 'A process is an executing program, a thread is a unit within it.'
      }
    };

    component.onSelectStudent(studentWithAnswers);
    expect(component.selectedStudent).toEqual(studentWithAnswers);
    expect(component.getStudentAnsweredCount(component.selectedStudent)).toBe(2);
    expect(component.isQuestionAnsweredByStudent(component.selectedStudent, 'q1')).toBeTrue();
    expect(component.isQuestionAnsweredByStudent(component.selectedStudent, 'q3')).toBeFalse();
    expect(component.getStudentAnswerText(component.selectedStudent, 'q1')).toBe('A process is an executing program, a thread is a unit within it.');
    expect(component.getStudentAnswerText(component.selectedStudent, 'q2')).toBeNull();

    component.onCloseStudentModal();
    expect(component.selectedStudent).toBeNull();
  });

  it('should send keepalive health ping when session is active', () => {
    component.classroom = { ...initialClassroom, active: true };
    component.sessionEnded = false;
    apiService.health.calls.reset();

    component.sendKeepAlivePing();
    expect(apiService.health).toHaveBeenCalled();
  });

  it('should not send keepalive health ping when session is ended', () => {
    apiService.health.calls.reset();
    component.sessionEnded = true;

    component.sendKeepAlivePing();
    expect(apiService.health).not.toHaveBeenCalled();
  });
});

