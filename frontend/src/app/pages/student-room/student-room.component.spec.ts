import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, Subject, throwError } from 'rxjs';
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

    apiSpy = jasmine.createSpyObj('ClassroomApiService', [
      'getClassroom',
      'completeQuestion',
      'leaveRoom',
      'createGroup',
      'addGroupMember',
      'leaveGroup'
    ]);
    apiSpy.getClassroom.and.returnValue(of(initialClassroom));
    apiSpy.leaveRoom.and.returnValue(of(initialClassroom));
    apiSpy.createGroup.and.returnValue(of(initialClassroom));
    apiSpy.addGroupMember.and.returnValue(of(initialClassroom));
    apiSpy.leaveGroup.and.returnValue(of(initialClassroom));

    const sessionSpy = jasmine.createSpyObj('SessionStorageService', ['getStudentSession', 'clearStudentSession']);
    sessionSpy.getStudentSession.and.returnValue({ studentId: 'my-student-id', studentName: 'Alice' });

    const socketSpy = jasmine.createSpyObj('ClassroomSocketService', ['connect', 'disconnect'], {
      connectionState$: of('CONNECTED'),
      events$: socketEventsSubject.asObservable(),
      reconnected$: new Subject<void>().asObservable()
    });

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule, FormsModule],
      declarations: [
        StudentRoomComponent,
        ConnectionIndicatorComponent,
        StatusLightComponent,
        ConfirmationDialogComponent
      ],
      providers: [
        { provide: ClassroomApiService, useValue: apiSpy },
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

  it('should allow immediate completion when written answers are disabled', () => {
    component.classroom = {
      ...initialClassroom,
      allowAnswers: false
    };

    expect(component.requiresWrittenAnswer).toBeFalse();
    expect(component.canComplete).toBeTrue();
  });

  it('should require non-empty text when written answers are enabled', () => {
    component.classroom = {
      ...initialClassroom,
      allowAnswers: true
    };

    expect(component.requiresWrittenAnswer).toBeTrue();
    component.answerText = '';
    expect(component.canComplete).toBeFalse();

    component.answerText = 'My detailed answer';
    expect(component.canComplete).toBeTrue();
  });

  it('should prevent paste and show warning', () => {
    const pasteEvent = new Event('paste') as ClipboardEvent;
    spyOn(pasteEvent, 'preventDefault');

    component.onPreventPaste(pasteEvent);

    expect(pasteEvent.preventDefault).toHaveBeenCalled();
    expect(component.pasteWarning).toContain('Copy-paste is disabled');
  });

  it('should create group when onCreateGroup is called', () => {
    component.classroom = {
      ...initialClassroom,
      groupsEnabled: true,
      maxGroupSize: 3,
      groups: []
    };
    component.newGroupName = 'Code Crafters';

    const updatedWithGroup: ClassroomSession = {
      ...component.classroom,
      groups: [
        {
          groupId: 'grp-99',
          groupName: 'Code Crafters',
          creatorStudentId: 'my-student-id',
          creatorStudentName: 'Alice',
          createdAt: new Date().toISOString(),
          members: [{ studentId: 'my-student-id', studentName: 'Alice', joinedAt: new Date().toISOString() }]
        }
      ]
    };
    apiSpy.createGroup.and.returnValue(of(updatedWithGroup));

    component.onCreateGroup();
    expect(apiSpy.createGroup).toHaveBeenCalledWith('AB7KQ2', 'my-student-id', 'Code Crafters');
    expect(component.myGroup?.groupId).toBe('grp-99');
    expect(component.newGroupName).toBe('');
  });

  it('should add member to group when onAddMember is called', () => {
    const group = {
      groupId: 'grp-99',
      groupName: 'Code Crafters',
      creatorStudentId: 'my-student-id',
      creatorStudentName: 'Alice',
      createdAt: new Date().toISOString(),
      members: [{ studentId: 'my-student-id', studentName: 'Alice', joinedAt: new Date().toISOString() }]
    };
    const targetStudent = {
      studentId: 's2',
      studentName: 'Bob',
      status: 'PENDING' as const,
      connected: true,
      joinedAt: new Date().toISOString(),
      completedAt: null
    };

    component.classroom = {
      ...initialClassroom,
      groupsEnabled: true,
      maxGroupSize: 3,
      groups: [group],
      students: [initialClassroom.students[0], targetStudent]
    };

    const updatedWithMember: ClassroomSession = {
      ...component.classroom,
      groups: [
        {
          ...group,
          members: [
            ...group.members,
            { studentId: 's2', studentName: 'Bob', joinedAt: new Date().toISOString() }
          ]
        }
      ]
    };
    apiSpy.addGroupMember.and.returnValue(of(updatedWithMember));

    component.onAddMember(targetStudent);
    expect(apiSpy.addGroupMember).toHaveBeenCalledWith('AB7KQ2', 'grp-99', 'my-student-id', 's2');
    expect(component.myGroup?.members.length).toBe(2);
  });

  it('should handle conflict error when adding a member already taken', () => {
    const group = {
      groupId: 'grp-99',
      groupName: 'Code Crafters',
      creatorStudentId: 'my-student-id',
      creatorStudentName: 'Alice',
      createdAt: new Date().toISOString(),
      members: [{ studentId: 'my-student-id', studentName: 'Alice', joinedAt: new Date().toISOString() }]
    };
    const targetStudent = {
      studentId: 's2',
      studentName: 'Bob',
      status: 'PENDING' as const,
      connected: true,
      joinedAt: new Date().toISOString(),
      completedAt: null
    };

    component.classroom = {
      ...initialClassroom,
      groupsEnabled: true,
      maxGroupSize: 3,
      groups: [group],
      students: [initialClassroom.students[0], targetStudent]
    };

    apiSpy.addGroupMember.and.returnValue(throwError(() => ({
      status: 409,
      error: { message: "Student 'Bob' has already been added to another group." }
    })));

    component.onAddMember(targetStudent);
    expect(component.groupErrorMessage).toBe("Student 'Bob' has already been added to another group.");
  });

  it('should leave group when onLeaveGroup is called', () => {
    const group = {
      groupId: 'grp-99',
      groupName: 'Code Crafters',
      creatorStudentId: 'my-student-id',
      creatorStudentName: 'Alice',
      createdAt: new Date().toISOString(),
      members: [{ studentId: 'my-student-id', studentName: 'Alice', joinedAt: new Date().toISOString() }]
    };

    component.classroom = {
      ...initialClassroom,
      groupsEnabled: true,
      maxGroupSize: 3,
      groups: [group]
    };

    const updatedWithoutGroup: ClassroomSession = {
      ...component.classroom,
      groups: []
    };
    apiSpy.leaveGroup.and.returnValue(of(updatedWithoutGroup));

    component.onLeaveGroup();
    expect(apiSpy.leaveGroup).toHaveBeenCalledWith('AB7KQ2', 'grp-99', 'my-student-id');
    expect(component.myGroup).toBeNull();
  });

  it('should filter available students by search text', () => {
    component.classroom = {
      ...initialClassroom,
      students: [
        { studentId: 'my-student-id', studentName: 'Alice', status: 'PENDING', connected: true, joinedAt: '', completedAt: null },
        { studentId: 's2', studentName: 'Charlie Brown', status: 'PENDING', connected: true, joinedAt: '', completedAt: null },
        { studentId: 's3', studentName: 'David Miller', status: 'PENDING', connected: true, joinedAt: '', completedAt: null }
      ]
    };

    expect(component.filteredAvailableStudents.length).toBe(2);

    component.studentSearchText = 'charlie';
    expect(component.filteredAvailableStudents.length).toBe(1);
    expect(component.filteredAvailableStudents[0].studentName).toBe('Charlie Brown');

    component.studentSearchText = 'xyz';
    expect(component.filteredAvailableStudents.length).toBe(0);
  });

  it('should reflect completion when teammate submits answer via WebSocket event', () => {
    component.classroom = {
      ...initialClassroom,
      groupsEnabled: true,
      maxGroupSize: 2,
      groups: [{
        groupId: 'grp-99',
        groupName: 'Alpha Team',
        creatorStudentId: 'teammate-id',
        creatorStudentName: 'Bob',
        createdAt: new Date().toISOString(),
        members: [
          { studentId: 'teammate-id', studentName: 'Bob', joinedAt: new Date().toISOString() },
          { studentId: 'my-student-id', studentName: 'Alice', joinedAt: new Date().toISOString() }
        ]
      }],
      students: [
        {
          studentId: 'my-student-id',
          studentName: 'Alice',
          status: 'PENDING',
          connected: true,
          joinedAt: '',
          completedAt: null,
          groupId: 'grp-99',
          groupName: 'Alpha Team'
        }
      ]
    };
    expect(component.isCompleted).toBeFalse();

    const teamUpdatedClassroom: ClassroomSession = {
      ...component.classroom,
      students: [
        {
          studentId: 'my-student-id',
          studentName: 'Alice',
          status: 'COMPLETED',
          connected: true,
          joinedAt: '',
          completedAt: new Date().toISOString(),
          groupId: 'grp-99',
          groupName: 'Alpha Team',
          answers: { 'q-1': 'Polymorphism allows objects to be treated as instances of their parent class.' }
        }
      ],
      completedCount: 1,
      pendingCount: 0
    };

    socketEventsSubject.next({
      type: 'STUDENT_STATUS_CHANGED',
      roomCode: 'AB7KQ2',
      timestamp: new Date().toISOString(),
      payload: teamUpdatedClassroom
    });

    expect(component.isCompleted).toBeTrue();
    expect(component.canComplete).toBeFalse();
    expect(component.mySubmittedAnswer).toBe('Polymorphism allows objects to be treated as instances of their parent class.');
  });
});

