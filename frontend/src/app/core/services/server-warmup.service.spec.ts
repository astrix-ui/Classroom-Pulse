import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ServerWarmupService } from './server-warmup.service';
import { environment } from '../../../environments/environment';

describe('ServerWarmupService', () => {
  let service: ServerWarmupService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ServerWarmupService]
    });

    service = TestBed.inject(ServerWarmupService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created and start in CHECKING state upon initWarmup', () => {
    service.initWarmup();
    let currentStatus = '';
    service.status$.subscribe(s => currentStatus = s);
    expect(currentStatus).toBe('CHECKING');

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/health`);
    expect(req.request.method).toBe('GET');
    req.flush({ status: 'UP' });
    expect(currentStatus).toBe('READY');
  });

  it('should transition to WAKING_UP if response takes longer than grace period', fakeAsync(() => {
    service.initWarmup();
    let currentStatus = '';
    service.status$.subscribe(s => currentStatus = s);

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/health`);

    // Advance past grace period (600ms)
    tick(700);
    expect(currentStatus).toBe('WAKING_UP');

    // Server finishes waking up and responds
    req.flush({ status: 'UP' });
    expect(currentStatus).toBe('READY');
  }));

  it('should transition to READY immediately if server responds quickly', fakeAsync(() => {
    service.initWarmup();
    let currentStatus = '';
    service.status$.subscribe(s => currentStatus = s);

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/health`);

    // Responds quickly in 200ms
    tick(200);
    req.flush({ status: 'UP' });
    expect(currentStatus).toBe('READY');

    // Advance further, should remain READY and not become WAKING_UP
    tick(1000);
    expect(currentStatus).toBe('READY');
  }));
});
