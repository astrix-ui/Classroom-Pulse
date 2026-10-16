import { ClassroomSession } from './classroom.model';

export type ClassroomEventType =
  | 'STUDENT_JOINED'
  | 'STUDENT_LEFT'
  | 'STUDENT_STATUS_CHANGED'
  | 'QUESTION_PUBLISHED'
  | 'QUESTION_UPDATED'
  | 'QUESTION_DELETED'
  | 'CLASSROOM_RESET'
  | 'CLASSROOM_CLOSED'
  | 'CLASSROOM_STATE_UPDATED'
  | 'ALLOW_ANSWERS_CHANGED'
  | 'GROUPS_UPDATED';

export interface WebSocketEvent<T = ClassroomSession> {
  type: ClassroomEventType;
  roomCode: string;
  timestamp: string;
  payload: T;
}

export type ConnectionState = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING';

