import { ClassroomSession } from './classroom.model';

export type ClassroomEventType =
  | 'STUDENT_JOINED'
  | 'STUDENT_STATUS_CHANGED'
  | 'QUESTION_PUBLISHED'
  | 'CLASSROOM_RESET'
  | 'CLASSROOM_CLOSED'
  | 'CLASSROOM_STATE_UPDATED';

export interface WebSocketEvent<T = ClassroomSession> {
  type: ClassroomEventType;
  roomCode: string;
  timestamp: string;
  payload: T;
}

export type ConnectionState = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING';

