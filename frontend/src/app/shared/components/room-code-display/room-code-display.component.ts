import { Component, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-room-code-display',
  templateUrl: './room-code-display.component.html',
  styleUrls: ['./room-code-display.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RoomCodeDisplayComponent {
  @Input() roomCode = '';
  @Input() showLabel = true;

  copied = false;
  private timeoutId: any;

  constructor(
    private cdr: ChangeDetectorRef,
    private toastService: ToastService
  ) {}

  copyCode(): void {
    if (!this.roomCode) return;

    if (navigator?.clipboard?.writeText) {
      navigator.clipboard.writeText(this.roomCode).then(() => {
        this.triggerCopied();
      }).catch(() => {
        this.fallbackCopy();
      });
    } else {
      this.fallbackCopy();
    }
  }

  private fallbackCopy(): void {
    try {
      const textarea = document.createElement('textarea');
      textarea.value = this.roomCode;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
      this.triggerCopied();
    } catch (e) {
      console.warn('Could not copy room code', e);
    }
  }

  private triggerCopied(): void {
    this.copied = true;
    this.toastService.info(`Room code ${this.roomCode} copied to clipboard!`);
    this.cdr.markForCheck();
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    this.timeoutId = setTimeout(() => {
      this.copied = false;
      this.cdr.markForCheck();
    }, 2000);
  }
}

