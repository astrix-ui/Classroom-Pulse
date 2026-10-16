import { Component, ChangeDetectionStrategy } from '@angular/core';
import { ToastService, ToastMessage } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  templateUrl: './toast-container.component.html',
  styleUrls: ['./toast-container.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToastContainerComponent {
  constructor(public toastService: ToastService) {}

  trackByToastId(index: number, toast: ToastMessage): string {
    return toast.id;
  }
}

