import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ServerWarmupService, ServerStatus } from './core/services/server-warmup.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'frontend';
  status: ServerStatus = 'CHECKING';
  elapsedSeconds = 0;
  showOverlay = false;
  isFadingOut = false;

  private destroy$ = new Subject<void>();

  constructor(
    private warmupService: ServerWarmupService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.warmupService.initWarmup();

    this.warmupService.status$
      .pipe(takeUntil(this.destroy$))
      .subscribe((st) => {
        this.status = st;

        if (st === 'WAKING_UP' || st === 'ERROR') {
          this.showOverlay = true;
          this.isFadingOut = false;
        } else if (st === 'READY') {
          if (this.showOverlay) {
            // Briefly show ready state, then fade out
            this.cdr.markForCheck();
            setTimeout(() => {
              this.isFadingOut = true;
              this.cdr.markForCheck();
              setTimeout(() => {
                this.showOverlay = false;
                this.isFadingOut = false;
                this.cdr.markForCheck();
              }, 300);
            }, 400);
          }
        }

        this.cdr.markForCheck();
      });

    this.warmupService.elapsedSeconds$
      .pipe(takeUntil(this.destroy$))
      .subscribe((sec) => {
        this.elapsedSeconds = sec;
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onRetry(): void {
    this.warmupService.retry();
  }
}
