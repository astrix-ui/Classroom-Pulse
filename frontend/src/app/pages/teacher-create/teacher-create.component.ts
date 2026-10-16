import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { ClassroomApiService } from '../../core/services/classroom-api.service';
import { SessionStorageService } from '../../core/services/session-storage.service';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-teacher-create',
  templateUrl: './teacher-create.component.html',
  styleUrls: ['./teacher-create.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TeacherCreateComponent {
  loading = false;
  errorMessage = '';

  constructor(
    private apiService: ClassroomApiService,
    private sessionStorageService: SessionStorageService,
    public themeService: ThemeService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  onCreateRoom(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.createClassroom().subscribe({
      next: (res) => {
        this.sessionStorageService.setTeacherToken(res.roomCode, res.teacherToken);
        this.router.navigate(['/teacher', res.roomCode]);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Failed to create classroom. Please ensure backend is reachable.';
        this.cdr.markForCheck();
      }
    });
  }
}

