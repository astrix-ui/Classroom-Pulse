import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// Pages
import { LandingComponent } from './pages/landing/landing.component';
import { TeacherCreateComponent } from './pages/teacher-create/teacher-create.component';
import { TeacherDashboardComponent } from './pages/teacher-dashboard/teacher-dashboard.component';
import { StudentJoinComponent } from './pages/student-join/student-join.component';
import { StudentRoomComponent } from './pages/student-room/student-room.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';

import { AboutComponent } from './pages/about/about.component';

// Shared Components
import { ConnectionIndicatorComponent } from './shared/components/connection-indicator/connection-indicator.component';
import { StatusLightComponent } from './shared/components/status-light/status-light.component';
import { RoomCodeDisplayComponent } from './shared/components/room-code-display/room-code-display.component';
import { LoadingSpinnerComponent } from './shared/components/loading-spinner/loading-spinner.component';
import { ConfirmationDialogComponent } from './shared/components/confirmation-dialog/confirmation-dialog.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ToastContainerComponent } from './shared/components/toast-container/toast-container.component';

@NgModule({
  declarations: [
    AppComponent,
    LandingComponent,
    AboutComponent,
    TeacherCreateComponent,
    TeacherDashboardComponent,
    StudentJoinComponent,
    StudentRoomComponent,
    NotFoundComponent,
    ConnectionIndicatorComponent,
    StatusLightComponent,
    RoomCodeDisplayComponent,
    LoadingSpinnerComponent,
    ConfirmationDialogComponent,
    FooterComponent,
    ToastContainerComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
