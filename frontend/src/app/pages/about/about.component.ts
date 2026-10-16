import { Component, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-about',
  templateUrl: './about.component.html',
  styleUrls: ['./about.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AboutComponent {
  currentYear = new Date().getFullYear();

  constructor(
    public themeService: ThemeService,
    private router: Router
  ) {}

  goToTeacher(): void {
    this.router.navigate(['/teacher/create']);
  }

  goToStudent(): void {
    this.router.navigate(['/student/join']);
  }

  goToHome(): void {
    this.router.navigate(['/']);
  }
}

