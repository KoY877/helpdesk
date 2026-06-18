import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { jwtInterceptor } from './core/interceptors/jwt-interceptor';

/**
 * Root application configuration: global error listeners, the router and the
 * HTTP client wired with the JWT interceptor.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Surface uncaught browser errors to Angular's error handling
    provideBrowserGlobalErrorListeners(),
    // Register the application routes
    provideRouter(routes),
    // Provide HttpClient and attach the JWT interceptor to every request
    provideHttpClient(withInterceptors([jwtInterceptor]))
  ]
};
