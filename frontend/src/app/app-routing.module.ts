import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { LandingComponent } from './pages/landing/landing.component';
import { TeacherCreateComponent } from './pages/teacher-create/teacher-create.component';
import { TeacherDashboardComponent } from './pages/teacher-dashboard/teacher-dashboard.component';
import { StudentJoinComponent } from './pages/student-join/student-join.component';
import { StudentRoomComponent } from './pages/student-room/student-room.component';
import { AboutComponent } from './pages/about/about.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';

import { teacherGuard } from './core/guards/teacher.guard';
import { studentGuard } from './core/guards/student.guard';

const routes: Routes = [
  { path: '', component: LandingComponent, pathMatch: 'full' },
  { path: 'about', component: AboutComponent },
  { path: 'teacher/create', component: TeacherCreateComponent },
  { path: 'teacher/:roomCode', component: TeacherDashboardComponent, canActivate: [teacherGuard] },
  { path: 'student/join', component: StudentJoinComponent },
  { path: 'student/:roomCode', component: StudentRoomComponent, canActivate: [studentGuard] },
  { path: '**', component: NotFoundComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: false })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
