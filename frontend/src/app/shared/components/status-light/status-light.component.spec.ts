import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusLightComponent } from './status-light.component';

describe('StatusLightComponent', () => {
  let component: StatusLightComponent;
  let fixture: ComponentFixture<StatusLightComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [StatusLightComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StatusLightComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should map to inactive/gray when no active question', () => {
    component.hasActiveQuestion = false;
    component.connected = true;
    component.status = 'PENDING';
    expect(component.mode).toBe('inactive');
    expect(component.statusText).toBe('Waiting for question');
  });

  it('should map to inactive/gray when disconnected', () => {
    component.hasActiveQuestion = true;
    component.connected = false;
    component.status = 'COMPLETED';
    expect(component.mode).toBe('inactive');
    expect(component.statusText).toBe('Disconnected');
  });

  it('should map to pending/amber when pending and question is active', () => {
    component.hasActiveQuestion = true;
    component.connected = true;
    component.status = 'PENDING';
    expect(component.mode).toBe('pending');
    expect(component.statusText).toBe('Pending');
  });

  it('should map to completed/green when completed and question is active', () => {
    component.hasActiveQuestion = true;
    component.connected = true;
    component.status = 'COMPLETED';
    expect(component.mode).toBe('completed');
    expect(component.statusText).toBe('Completed');
  });
});

