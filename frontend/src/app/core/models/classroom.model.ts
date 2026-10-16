import { QuestionDto } from './question.model';
import { StudentDto } from './student-status.model';

export interface GroupMemberDto {
  studentId: string;
  studentName: string;
  joinedAt: string;
}

export interface StudentGroupDto {
  groupId: string;
  groupName: string;
  creatorStudentId: string;
  creatorStudentName: string;
  createdAt: string;
  members: GroupMemberDto[];
}

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
  groupsEnabled?: boolean;
  maxGroupSize?: number;
  groups?: StudentGroupDto[];
  createdAt: string;
}

export interface CreateClassroomResponse {
  roomCode: string;
  teacherToken: string;
}

