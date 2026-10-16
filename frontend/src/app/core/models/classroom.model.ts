import { QuestionDto } from './question.model';
import { StudentDto } from './student-status.model';

export interface ClassroomSession {
  roomCode: string;
  active: boolean;
  currentQuestion: QuestionDto | null;
  students: StudentDto[];
  totalStudents: number;
  completedCount: number;
  pendingCount: number;
  completionPercentage: number;
  allowAnswers?: boolean;
  questions?: QuestionDto[];
  createdAt: string;
}

export interface CreateClassroomResponse {
  roomCode: string;
  teacherToken: string;
}

