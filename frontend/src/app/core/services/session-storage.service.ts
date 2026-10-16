import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SessionStorageService {

  private readonly TEACHER_TOKEN_PREFIX = 'cls_teacher_token_';
  private readonly STUDENT_ID_PREFIX = 'cls_student_id_';
  private readonly STUDENT_NAME_PREFIX = 'cls_student_name_';

  setTeacherToken(roomCode: string, token: string): void {
    if (this.isStorageAvailable()) {
      sessionStorage.setItem(`${this.TEACHER_TOKEN_PREFIX}${roomCode.toUpperCase()}`, token);
    }
  }

  getTeacherToken(roomCode: string): string | null {
    if (this.isStorageAvailable()) {
      return sessionStorage.getItem(`${this.TEACHER_TOKEN_PREFIX}${roomCode.toUpperCase()}`);
    }
    return null;
  }

  removeTeacherToken(roomCode: string): void {
    if (this.isStorageAvailable()) {
      sessionStorage.removeItem(`${this.TEACHER_TOKEN_PREFIX}${roomCode.toUpperCase()}`);
    }
  }

  setStudentSession(roomCode: string, studentId: string, studentName: string): void {
    if (this.isStorageAvailable()) {
      const code = roomCode.toUpperCase();
      sessionStorage.setItem(`${this.STUDENT_ID_PREFIX}${code}`, studentId);
      sessionStorage.setItem(`${this.STUDENT_NAME_PREFIX}${code}`, studentName);
    }
  }

  getStudentSession(roomCode: string): { studentId: string; studentName: string } | null {
    if (this.isStorageAvailable()) {
      const code = roomCode.toUpperCase();
      const studentId = sessionStorage.getItem(`${this.STUDENT_ID_PREFIX}${code}`);
      const studentName = sessionStorage.getItem(`${this.STUDENT_NAME_PREFIX}${code}`);
      if (studentId && studentName) {
        return { studentId, studentName };
      }
    }
    return null;
  }

  clearStudentSession(roomCode: string): void {
    if (this.isStorageAvailable()) {
      const code = roomCode.toUpperCase();
      sessionStorage.removeItem(`${this.STUDENT_ID_PREFIX}${code}`);
      sessionStorage.removeItem(`${this.STUDENT_NAME_PREFIX}${code}`);
    }
  }

  private isStorageAvailable(): boolean {
    return typeof window !== 'undefined' && typeof window.sessionStorage !== 'undefined';
  }
}

