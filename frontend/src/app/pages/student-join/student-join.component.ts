import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ClassroomApiService } from '../../core/services/classroom-api.service';
import { SessionStorageService } from '../../core/services/session-storage.service';

@Component({
  selector: 'app-student-join',
  templateUrl: './student-join.component.html',
  styleUrls: ['./student-join.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StudentJoinComponent implements OnInit {

  joinForm = new FormGroup({
    roomCode: new FormControl('', [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(6)
    ]),
    studentName: new FormControl('', [
      Validators.required,
      Validators.minLength(2),
      Validators.maxLength(50)
    ])
  });

  loading = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ClassroomApiService,
    private sessionStorageService: SessionStorageService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const codeParam = this.route.snapshot.queryParamMap.get('roomCode');
    if (codeParam) {
      this.joinForm.patchValue({ roomCode: codeParam.toUpperCase().trim() });
    }

    const errorParam = this.route.snapshot.queryParamMap.get('error');
    if (errorParam === 'session_expired') {
      this.errorMessage = 'Your previous session was not found. Please rejoin.';
    }
  }

  get roomCodeControl(): FormControl {
    return this.joinForm.get('roomCode') as FormControl;
  }

  get studentNameControl(): FormControl {
    return this.joinForm.get('studentName') as FormControl;
  }

  onJoin(): void {
    if (this.joinForm.invalid || this.loading) {
      this.joinForm.markAllAsTouched();
      return;
    }

    const roomCode = (this.roomCodeControl.value || '').toUpperCase().trim();
    const studentName = (this.studentNameControl.value || '').trim();

    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.apiService.joinStudent(roomCode, studentName).subscribe({
      next: (res) => {
        this.sessionStorageService.setStudentSession(roomCode, res.studentId, res.studentName);
        this.router.navigate(['/student', roomCode]);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 404) {
          this.errorMessage = 'Classroom not found. Please check your 6-character room code.';
        } else if (err.status === 409) {
          this.errorMessage = err?.error?.message || 'A student with this name has already joined, or session is closed.';
        } else {
          this.errorMessage = err?.error?.message || 'Failed to join classroom. Please try again.';
        }
        this.cdr.markForCheck();
      }
    });
  }
}

