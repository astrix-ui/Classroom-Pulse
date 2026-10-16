import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
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

    const apiSpy = jasmine.createSpyObj('ClassroomApiService', [
      'getClassroom',
      'publishQuestion',
      'editQuestion',
      'deleteQuestion',
      'resetClassroom',
      'closeClassroom'
    ]);
    apiSpy.getClassroom.and.returnValue(of(initialClassroom));
    apiSpy.deleteQuestion.and.returnValue(of(initialClassroom));

    const sessionSpy = jasmine.createSpyObj('SessionStorageService', ['getTeacherToken', 'removeTeacherToken']);
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
        { provide: ClassroomApiService, useValue: apiSpy },
        { provide: SessionStorageService, useValue: sessionSpy },
        { provide: ClassroomSocketService, useValue: socketSpy }
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
});

