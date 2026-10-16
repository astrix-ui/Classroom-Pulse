import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, Subject } from 'rxjs';
import { StudentRoomComponent } from './student-room.component';
import { ClassroomApiService } from '../../core/services/classroom-api.service';
import { ClassroomSocketService } from '../../core/services/classroom-socket.service';
import { SessionStorageService } from '../../core/services/session-storage.service';
import { ConnectionIndicatorComponent } from '../../shared/components/connection-indicator/connection-indicator.component';
import { StatusLightComponent } from '../../shared/components/status-light/status-light.component';
import { ConfirmationDialogComponent } from '../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ClassroomSession } from '../../core/models/classroom.model';
import { WebSocketEvent } from '../../core/models/websocket-event.model';

describe('StudentRoomComponent', () => {
  let component: StudentRoomComponent;
  let fixture: ComponentFixture<StudentRoomComponent>;
  let socketEventsSubject: Subject<WebSocketEvent<ClassroomSession>>;
  let apiSpy: jasmine.SpyObj<ClassroomApiService>;

  const initialClassroom: ClassroomSession = {
    roomCode: 'AB7KQ2',
    active: true,
    currentQuestion: {
      questionId: 'q-1',
      questionText: 'What is an Observable?',
      sequenceNumber: 1,
      publishedAt: new Date().toISOString()
    },
    students: [
      {
        studentId: 'my-student-id',
        studentName: 'Alice',
        status: 'PENDING',
        connected: true,
        joinedAt: new Date().toISOString(),
        completedAt: null
      }
    ],
    totalStudents: 1,
    completedCount: 0,
    pendingCount: 1,
    completionPercentage: 0,
    createdAt: new Date().toISOString()
  };

  beforeEach(async () => {
    socketEventsSubject = new Subject<WebSocketEvent<ClassroomSession>>();

    apiSpy = jasmine.createSpyObj('ClassroomApiService', ['getClassroom', 'completeQuestion', 'leaveRoom']);
    apiSpy.getClassroom.and.returnValue(of(initialClassroom));
    apiSpy.leaveRoom.and.returnValue(of(initialClassroom));

    const sessionSpy = jasmine.createSpyObj('SessionStorageService', ['getStudentSession', 'clearStudentSession']);
    sessionSpy.getStudentSession.and.returnValue({ studentId: 'my-student-id', studentName: 'Alice' });

    const socketSpy = jasmine.createSpyObj('ClassroomSocketService', ['connect', 'disconnect'], {
      connectionState$: of('CONNECTED'),
      events$: socketEventsSubject.asObservable(),
      reconnected$: new Subject<void>().asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [
        StudentRoomComponent,
        ConnectionIndicatorComponent,
        StatusLightComponent,
        ConfirmationDialogComponent
      ],
      providers: [
        { provide: ClassroomApiService, useValue: apiSpy },
        { provide: SessionStorageService, useValue: sessionSpy },
        { provide: ClassroomSocketService, useValue: socketSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentRoomComponent);
    component = fixture.componentInstance;
    component.roomCode = 'AB7KQ2';
    component.studentId = 'my-student-id';
    component.studentName = 'Alice';
    component.classroom = initialClassroom;
    fixture.detectChanges();
  });

  it('should enable COMPLETED button when question is active and status is pending', () => {
    expect(component.canComplete).toBeTrue();
    expect(component.isCompleted).toBeFalse();
  });

  it('should disable COMPLETED button after successful completion', () => {
    const completedClassroom: ClassroomSession = {
      ...initialClassroom,
      students: [
        {
          studentId: 'my-student-id',
          studentName: 'Alice',
          status: 'COMPLETED',
          connected: true,
          joinedAt: new Date().toISOString(),
          completedAt: new Date().toISOString()
        }
      ],
      completedCount: 1,
      pendingCount: 0,
      completionPercentage: 100
    };

    apiSpy.completeQuestion.and.returnValue(of(completedClassroom));

    component.onMarkCompleted();

    expect(component.isCompleted).toBeTrue();
    expect(component.canComplete).toBeFalse();
  });

  it('should reset student UI when a new question is published', () => {
    // Student was completed on question 1
    component.classroom = {
      ...initialClassroom,
      students: [{ ...initialClassroom.students[0], status: 'COMPLETED' }]
    };
    expect(component.isCompleted).toBeTrue();

    // Teacher publishes question 2
    const nextQuestionClassroom: ClassroomSession = {
      ...initialClassroom,
      currentQuestion: {
        questionId: 'q-2',
        questionText: 'What is RxJS pipe?',
        sequenceNumber: 2,
        publishedAt: new Date().toISOString()
      },
      students: [{ ...initialClassroom.students[0], status: 'PENDING', completedAt: null }]
    };

    socketEventsSubject.next({
      type: 'QUESTION_PUBLISHED',
      roomCode: 'AB7KQ2',
      timestamp: new Date().toISOString(),
      payload: nextQuestionClassroom
    });

    expect(component.isCompleted).toBeFalse();
    expect(component.canComplete).toBeTrue();
    expect(component.currentQuestion?.sequenceNumber).toBe(2);
  });

  it('should disable student actions when classroom closed event is received', () => {
    const closedClassroom: ClassroomSession = {
      ...initialClassroom,
      active: false
    };

    socketEventsSubject.next({
      type: 'CLASSROOM_CLOSED',
      roomCode: 'AB7KQ2',
      timestamp: new Date().toISOString(),
      payload: closedClassroom
    });

    expect(component.isClassroomActive).toBeFalse();
    expect(component.canComplete).toBeFalse();
  });
});

