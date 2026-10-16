import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, BehaviorSubject } from 'rxjs';
import { AppComponent } from './app.component';
import { ServerWarmupService, ServerStatus } from './core/services/server-warmup.service';

describe('AppComponent', () => {
  let warmupSpy: jasmine.SpyObj<ServerWarmupService>;
  let statusSubject: BehaviorSubject<ServerStatus>;
  let elapsedSubject: BehaviorSubject<number>;

  beforeEach(() => {
    statusSubject = new BehaviorSubject<ServerStatus>('READY');
    elapsedSubject = new BehaviorSubject<number>(0);

    warmupSpy = jasmine.createSpyObj('ServerWarmupService', ['initWarmup', 'retry'], {
      status$: statusSubject.asObservable(),
      elapsedSeconds$: elapsedSubject.asObservable()
    });

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [AppComponent],
      providers: [
        { provide: ServerWarmupService, useValue: warmupSpy }
      ]
    });
  });

  it('should create the app and trigger warmup check', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
    fixture.detectChanges();
    expect(warmupSpy.initWarmup).toHaveBeenCalled();
  });

  it(`should have as title 'frontend'`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('frontend');
  });

  it('should have a router outlet', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });

  it('should display warmup overlay when server is waking up', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    statusSubject.next('WAKING_UP');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.server-warmup-overlay')).toBeTruthy();
    expect(compiled.textContent).toContain('Connecting to Cloud Server');
  });

  it('should skip warmup overlay when server responds as ready', () => {
    statusSubject.next('READY');
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.server-warmup-overlay')).toBeFalsy();
  });
});
