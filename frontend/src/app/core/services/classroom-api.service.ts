import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClassroomSession, CreateClassroomResponse } from '../models/classroom.model';
import { JoinStudentResponse } from '../models/student-status.model';
import { QuestionDto } from '../models/question.model';

@Injectable({
  providedIn: 'root'
})
export class ClassroomApiService {

  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  createClassroom(): Observable<CreateClassroomResponse> {
    return this.http.post<CreateClassroomResponse>(`${this.baseUrl}/classrooms`, {});
  }

  getClassroom(roomCode: string): Observable<ClassroomSession> {
    return this.http.get<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}`);
  }

  joinStudent(roomCode: string, studentName: string): Observable<JoinStudentResponse> {
    return this.http.post<JoinStudentResponse>(`${this.baseUrl}/classrooms/${roomCode}/students`, {
      studentName
    });
  }

  publishQuestion(roomCode: string, teacherToken: string, questionText: string): Observable<QuestionDto> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.post<QuestionDto>(`${this.baseUrl}/classrooms/${roomCode}/questions`, {
      questionText
    }, { headers });
  }

  completeQuestion(roomCode: string, studentId: string, questionId: string, answerText?: string): Observable<ClassroomSession> {
    const payload: { questionId: string; answerText?: string } = { questionId };
    if (answerText && answerText.trim()) {
      payload.answerText = answerText.trim();
    }
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/students/${studentId}/complete`, payload);
  }

  resetClassroom(roomCode: string, teacherToken: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/reset`, {}, { headers });
  }

  leaveRoom(roomCode: string, studentId: string): Observable<ClassroomSession> {
    return this.http.delete<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/students/${studentId}`);
  }

  editQuestion(roomCode: string, teacherToken: string, questionText: string): Observable<QuestionDto> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.put<QuestionDto>(`${this.baseUrl}/classrooms/${roomCode}/questions`, {
      questionText
    }, { headers });
  }

  deleteQuestion(roomCode: string, teacherToken: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.delete<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/questions`, { headers });
  }

  toggleAllowAnswers(roomCode: string, teacherToken: string, allowAnswers: boolean): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.put<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/allow-answers`, {
      allowAnswers
    }, { headers });
  }

  closeClassroom(roomCode: string, teacherToken: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/close`, {}, { headers });
  }

  health(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${this.baseUrl}/health`);
  }

  configureGroups(roomCode: string, teacherToken: string, enabled: boolean, maxGroupSize: number): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.put<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups/settings`, {
      enabled,
      maxGroupSize
    }, { headers });
  }

  resetGroups(roomCode: string, teacherToken: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups/reset`, {}, { headers });
  }

  createGroup(roomCode: string, studentId: string, groupName?: string): Observable<ClassroomSession> {
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups`, {
      studentId,
      groupName
    });
  }

  addGroupMember(roomCode: string, groupId: string, requestingStudentId: string, targetStudentId: string): Observable<ClassroomSession> {
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups/${groupId}/members`, {
      requestingStudentId,
      targetStudentId
    });
  }

  leaveGroup(roomCode: string, groupId: string, studentId: string): Observable<ClassroomSession> {
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups/${groupId}/members/${studentId}/leave`, {});
  }

  removeStudentFromGroupByTeacher(roomCode: string, teacherToken: string, groupId: string, studentId: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.delete<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups/${groupId}/members/${studentId}`, { headers });
  }

  assignStudentToGroupByTeacher(roomCode: string, teacherToken: string, groupId: string, studentId: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.post<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups/${groupId}/assign`, {
      studentId
    }, { headers });
  }

  kickStudent(roomCode: string, teacherToken: string, studentId: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.delete<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/students/${studentId}/kick`, { headers });
  }

  renameGroup(roomCode: string, teacherToken: string, groupId: string, groupName: string): Observable<ClassroomSession> {
    const headers = new HttpHeaders({
      'X-Teacher-Token': teacherToken
    });
    return this.http.put<ClassroomSession>(`${this.baseUrl}/classrooms/${roomCode}/groups/${groupId}/name`, {
      groupName
    }, { headers });
  }
}

