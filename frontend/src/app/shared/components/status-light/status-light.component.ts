import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CompletionStatus } from '../../../core/models/student-status.model';

@Component({
  selector: 'app-status-light',
  templateUrl: './status-light.component.html',
  styleUrls: ['./status-light.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusLightComponent {
  @Input() status: CompletionStatus | null = 'PENDING';
  @Input() hasActiveQuestion = true;
  @Input() connected = true;
  @Input() showText = true;

  get mode(): 'inactive' | 'pending' | 'completed' {
    if (!this.connected || !this.hasActiveQuestion) {
      return 'inactive';
    }
    return this.status === 'COMPLETED' ? 'completed' : 'pending';
  }

  get statusText(): string {
    if (!this.connected) return 'Disconnected';
    if (!this.hasActiveQuestion) return 'Waiting for question';
    return this.status === 'COMPLETED' ? 'Completed' : 'Pending';
  }
}

