import { Injectable, NgZone } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ClassroomSession } from '../models/classroom.model';
import { ConnectionState, WebSocketEvent } from '../models/websocket-event.model';

@Injectable({
  providedIn: 'root'
})
export class ClassroomSocketService {

  private client: Client | null = null;
  private currentSubscription: StompSubscription | null = null;
  private currentRoomCode: string | null = null;
  private hasConnectedOnce = false;

  private readonly connectionStateSubject = new BehaviorSubject<ConnectionState>('DISCONNECTED');
  readonly connectionState$ = this.connectionStateSubject.asObservable();

  private readonly eventsSubject = new Subject<WebSocketEvent<ClassroomSession>>();
  readonly events$ = this.eventsSubject.asObservable();

  private readonly reconnectedSubject = new Subject<void>();
  readonly reconnected$ = this.reconnectedSubject.asObservable();

  constructor(private zone: NgZone) {}

  connect(roomCode: string): void {
    const normalizedCode = roomCode.toUpperCase();

    // If already connected to this room, no need to reconnect
    if (this.client && this.client.active && this.currentRoomCode === normalizedCode) {
      return;
    }

    // Clean up previous connection if switching rooms
    this.disconnect();
    this.currentRoomCode = normalizedCode;
    this.hasConnectedOnce = false;

    this.connectionStateSubject.next('CONNECTING');

    this.client = new Client({
      brokerURL: environment.webSocketUrl,
      reconnectDelay: 3000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (msg: string) => {
        // Helpful during troubleshooting, low overhead
        // console.debug('[STOMP]', msg);
      }
    });

    this.client.onConnect = () => {
      this.zone.run(() => {
        const wasReconnecting = this.hasConnectedOnce;
        this.hasConnectedOnce = true;
        this.connectionStateSubject.next('CONNECTED');

        this.subscribeToRoom(normalizedCode);

        if (wasReconnecting) {
          this.reconnectedSubject.next();
        }
      });
    };

    this.client.onDisconnect = () => {
      this.zone.run(() => {
        this.connectionStateSubject.next('DISCONNECTED');
      });
    };

    this.client.onWebSocketClose = () => {
      this.zone.run(() => {
        if (this.hasConnectedOnce) {
          this.connectionStateSubject.next('RECONNECTING');
        } else {
          this.connectionStateSubject.next('DISCONNECTED');
        }
      });
    };

    this.client.onStompError = (frame) => {
      this.zone.run(() => {
        console.error('STOMP error:', frame.headers['message'], frame.body);
        this.connectionStateSubject.next('DISCONNECTED');
      });
    };

    this.client.onWebSocketError = (error) => {
      this.zone.run(() => {
        console.error('WebSocket connection error:', error);
        if (this.hasConnectedOnce) {
          this.connectionStateSubject.next('RECONNECTING');
        } else {
          this.connectionStateSubject.next('DISCONNECTED');
        }
      });
    };

    this.client.activate();
  }

  private subscribeToRoom(roomCode: string): void {
    if (!this.client || !this.client.connected) {
      return;
    }

    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
    }

    const topic = `/topic/classrooms/${roomCode}`;
    this.currentSubscription = this.client.subscribe(topic, (message: IMessage) => {
      this.zone.run(() => {
        try {
          const event: WebSocketEvent<ClassroomSession> = JSON.parse(message.body);
          this.eventsSubject.next(event);
        } catch (err) {
          console.error('Failed to parse WebSocket message body:', err);
        }
      });
    });
  }

  disconnect(): void {
    if (this.currentSubscription) {
      this.currentSubscription.unsubscribe();
      this.currentSubscription = null;
    }
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.currentRoomCode = null;
    this.hasConnectedOnce = false;
    this.connectionStateSubject.next('DISCONNECTED');
  }

  getConnectionState(): ConnectionState {
    return this.connectionStateSubject.getValue();
  }
}

