import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ToastType = 'success' | 'info' | 'warning' | 'error';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title?: string;
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  public toasts$: Observable<ToastMessage[]> = this.toastsSubject.asObservable();

  private counter = 0;

  show(message: string, type: ToastType = 'info', title?: string, duration: number = 4000): void {
    const id = `toast-${++this.counter}-${Date.now()}`;
    const newToast: ToastMessage = { id, type, title, message, duration };

    // Keep max 5 concurrent toasts
    const current = this.toastsSubject.value;
    const updated = [newToast, ...current].slice(0, 5);
    this.toastsSubject.next(updated);

    if (duration > 0) {
      setTimeout(() => {
        this.dismiss(id);
      }, duration);
    }
  }

  success(message: string, title?: string, duration: number = 3500): void {
    this.show(message, 'success', title, duration);
  }

  info(message: string, title?: string, duration: number = 3500): void {
    this.show(message, 'info', title, duration);
  }

  warning(message: string, title?: string, duration: number = 4500): void {
    this.show(message, 'warning', title, duration);
  }

  error(message: string, title?: string, duration: number = 5000): void {
    this.show(message, 'error', title, duration);
  }

  dismiss(id: string): void {
    const current = this.toastsSubject.value;
    this.toastsSubject.next(current.filter(t => t.id !== id));
  }

  clearAll(): void {
    this.toastsSubject.next([]);
  }
}

