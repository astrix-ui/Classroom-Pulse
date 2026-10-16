import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { StudentJoinComponent } from './student-join.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

describe('StudentJoinComponent', () => {
  let component: StudentJoinComponent;
  let fixture: ComponentFixture<StudentJoinComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, RouterTestingModule, HttpClientTestingModule],
      declarations: [StudentJoinComponent, LoadingSpinnerComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StudentJoinComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with invalid empty form', () => {
    expect(component.joinForm.valid).toBeFalse();
  });

  it('should reject roomCode shorter than 6 characters', () => {
    component.joinForm.patchValue({ roomCode: 'AB12', studentName: 'Alice' });
    expect(component.joinForm.valid).toBeFalse();
    expect(component.roomCodeControl.errors?.['minlength']).toBeTruthy();
  });

  it('should reject student name shorter than 2 characters', () => {
    component.joinForm.patchValue({ roomCode: 'AB7KQ2', studentName: 'A' });
    expect(component.joinForm.valid).toBeFalse();
    expect(component.studentNameControl.errors?.['minlength']).toBeTruthy();
  });

  it('should validate successfully with valid 6-char room code and name', () => {
    component.joinForm.patchValue({ roomCode: 'AB7KQ2', studentName: 'Alice' });
    expect(component.joinForm.valid).toBeTrue();
  });
});

