import { addIcons } from 'ionicons';
import {
  homeOutline, home,
  calendarOutline, calendar,
  documentTextOutline, documentText,
  personOutline, person,
} from 'ionicons/icons';

addIcons({
  'home-outline': homeOutline, home,
  'calendar-outline': calendarOutline, calendar,
  'document-text-outline': documentTextOutline, 'document-text': documentText,
  'person-outline': personOutline, person,
});
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { inject, provideAppInitializer } from '@angular/core';
import { authInterceptor } from './app/core/auth.interceptor';
import { AuthService } from './app/core/auth.service';
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
    provideAppInitializer(() => inject(AuthService).init()),
  ],
});
