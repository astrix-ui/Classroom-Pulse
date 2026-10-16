import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { SessionStorageService } from '../services/session-storage.service';

export const teacherGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const sessionStorageService = inject(SessionStorageService);

  const roomCode = route.paramMap.get('roomCode');
  if (!roomCode) {
    router.navigate(['/teacher/create']);
    return false;
  }

  const token = sessionStorageService.getTeacherToken(roomCode);
  if (!token) {
    router.navigate(['/teacher/create'], { queryParams: { error: 'session_expired' } });
    return false;
  }

  return true;
};

