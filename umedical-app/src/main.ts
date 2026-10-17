import { addIcons } from 'ionicons';
import {
  homeOutline, home,
  calendarOutline, calendar,
  documentTextOutline, documentText,
  personOutline, person,
  timeOutline,
  checkmarkCircleOutline,
  eyeOutline, eyeOffOutline,
  lockClosedOutline,
  medicalOutline,
} from 'ionicons/icons';

addIcons({
  'home-outline': homeOutline, home,
  'calendar-outline': calendarOutline, calendar,
  'document-text-outline': documentTextOutline, 'document-text': documentText,
  'person-outline': personOutline, person,
  'time-outline': timeOutline,
  'checkmark-circle-outline': checkmarkCircleOutline,
  'eye-outline': eyeOutline, 'eye-off-outline': eyeOffOutline,
  'lock-closed-outline': lockClosedOutline,
  'medical-outline': medicalOutline,
});

import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './app/core/auth.interceptor';
import { bootstrapApplication } from '@angular/platform-browser';
import { RouteReuseStrategy, provideRouter, withComponentInputBinding, withPreloading, PreloadAllModules } from '@angular/router';
import { IonicRouteStrategy, provideIonicAngular } from '@ionic/angular';

import { routes } from './app/app.routes';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, {
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    provideIonicAngular(),
    provideRouter(routes, withPreloading(PreloadAllModules), withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
});