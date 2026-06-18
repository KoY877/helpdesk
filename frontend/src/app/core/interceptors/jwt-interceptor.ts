import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/AuthService';

/**
 * HTTP interceptor that attaches the JWT to outgoing requests and reacts to
 * authentication errors by logging the user out and redirecting to login.
 *
 * @param req the outgoing HTTP request
 * @param next the next handler in the interceptor chain
 * @returns the (possibly modified) request stream
 */
export const jwtInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  // Resolve the dependencies via Angular's functional injection
  const authService = inject(AuthService);
  const router = inject(Router);
  const token = authService.getToken();

  // When a token exists, clone the request and add the Authorization header
  const authReq = token
    ? req.clone({ headers: req.headers.append('Authorization', `Bearer ${token}`) })
    : req;

  // Forward the request, intercepting auth failures along the way
  return next(authReq).pipe(
    catchError(err => {
      // 401/403 means the session is invalid: clear it and send the user to login
      if (err.status === 401 || err.status === 403) {
        authService.logout();
        router.navigate(['/login']);
      }
      // Re-throw so callers can still handle the error
      return throwError(() => err);
    })
  );
};
