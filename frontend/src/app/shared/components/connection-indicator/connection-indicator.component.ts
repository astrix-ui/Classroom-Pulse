import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { ConnectionState } from '../../../core/models/websocket-event.model';

@Component({
  selector: 'app-connection-indicator',
  templateUrl: './connection-indicator.component.html',
  styleUrls: ['./connection-indicator.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConnectionIndicatorComponent {
  @Input() state: ConnectionState = 'DISCONNECTED';

  get label(): string {
    switch (this.state) {
      case 'CONNECTED':
        return 'Live';
      case 'CONNECTING':
        return 'Connecting...';
      case 'RECONNECTING':
        return 'Reconnecting...';
      case 'DISCONNECTED':
      default:
        return 'Disconnected';
    }
  }

  get stateClass(): string {
    switch (this.state) {
      case 'CONNECTED':
        return 'state-connected';
      case 'CONNECTING':
      case 'RECONNECTING':
        return 'state-warning';
      case 'DISCONNECTED':
      default:
        return 'state-disconnected';
    }
  }
}

