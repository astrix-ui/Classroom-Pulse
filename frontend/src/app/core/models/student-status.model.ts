export type CompletionStatus = 'PENDING' | 'COMPLETED';

export interface StudentDto {
  studentId: string;
  studentName: string;
  status: CompletionStatus;
  connected: boolean;
  joinedAt: string;
  completedAt: string | null;
}

export interface JoinStudentRequest {
  studentName: string;
}

export interface JoinStudentResponse {
  studentId: string;
  studentName: string;
  roomCode: string;
  status: CompletionStatus;
}

