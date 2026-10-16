import { Component, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingComponent {
  constructor(
    private router: Router,
    public themeService: ThemeService
  ) {}

  goToTeacher(): void {
    this.router.navigate(['/teacher/create']);
  }

  goToStudent(): void {
    this.router.navigate(['/student/join']);
  }
}

