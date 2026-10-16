import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { SessionStorageService } from '../services/session-storage.service';

export const studentGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const sessionStorageService = inject(SessionStorageService);

  const roomCode = route.paramMap.get('roomCode');
  if (!roomCode) {
    router.navigate(['/student/join']);
    return false;
  }

  const session = sessionStorageService.getStudentSession(roomCode);
  if (!session) {
    router.navigate(['/student/join'], { queryParams: { roomCode: roomCode.toUpperCase(), error: 'session_expired' } });
    return false;
  }

  return true;
};

