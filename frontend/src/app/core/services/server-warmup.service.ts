import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Subscription, timer, of } from 'rxjs';
import { timeout, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export type ServerStatus = 'CHECKING' | 'WAKING_UP' | 'READY' | 'ERROR';

@Injectable({
  providedIn: 'root'
})
export class ServerWarmupService {
  private readonly baseUrl = environment.apiBaseUrl;

  private statusSubject = new BehaviorSubject<ServerStatus>('CHECKING');
  public status$ = this.statusSubject.asObservable();

  private elapsedSecondsSubject = new BehaviorSubject<number>(0);
  public elapsedSeconds$ = this.elapsedSecondsSubject.asObservable();

  private timerSub?: Subscription;
  private graceSub?: Subscription;
  private pollSub?: Subscription;
  private elapsedSeconds = 0;
  private readonly GRACE_PERIOD_MS = 600;
  private readonly MAX_WAIT_SECONDS = 90;

  constructor(private http: HttpClient) {}

  public initWarmup(): void {
    if (this.statusSubject.value === 'READY') {
      return;
    }

    this.elapsedSeconds = 0;
    this.statusSubject.next('CHECKING');
    this.elapsedSecondsSubject.next(0);

    // After grace period, if still checking, show waking up state
    this.graceSub?.unsubscribe();
    this.graceSub = timer(this.GRACE_PERIOD_MS).subscribe(() => {
      if (this.statusSubject.value === 'CHECKING') {
        this.statusSubject.next('WAKING_UP');
      }
    });

    // Timer to track elapsed seconds
    this.timerSub?.unsubscribe();
    this.timerSub = timer(1000, 1000).subscribe(() => {
      this.elapsedSeconds++;
      this.elapsedSecondsSubject.next(this.elapsedSeconds);

      if (this.elapsedSeconds >= this.MAX_WAIT_SECONDS && this.statusSubject.value !== 'READY') {
        this.statusSubject.next('ERROR');
        this.stopPolling();
      }
    });

    this.startPinging();
  }

  private startPinging(): void {
    this.pollSub?.unsubscribe();
    this.attemptPing();
  }

  private attemptPing(): void {
    if (this.statusSubject.value === 'READY' || this.statusSubject.value === 'ERROR') {
      return;
    }

    this.http.get<{ status: string }>(`${this.baseUrl}/health`)
      .pipe(
        timeout(10000),
        catchError(() => of(null))
      )
      .subscribe({
        next: (res) => {
          if (res && res.status === 'UP') {
            this.handleSuccess();
          } else {
            if (this.elapsedSeconds < this.MAX_WAIT_SECONDS && this.statusSubject.value !== 'READY') {
              this.pollSub = timer(2500).subscribe(() => this.attemptPing());
            }
          }
        }
      });
  }

  private handleSuccess(): void {
    this.stopPolling();
    this.statusSubject.next('READY');
  }

  private stopPolling(): void {
    this.graceSub?.unsubscribe();
    this.pollSub?.unsubscribe();
    this.timerSub?.unsubscribe();
  }

  public retry(): void {
    this.stopPolling();
    this.initWarmup();
  }
}
