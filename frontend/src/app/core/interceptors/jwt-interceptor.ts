import { HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/AuthService';

// Auth endpoints must never be retried through the refresh flow, otherwise a
// failing login/register/refresh call would try to refresh itself forever
const isAuthEndpoint = (url: string): boolean =>
  url.includes('/auth/login') || url.includes('/auth/register') ||
  url.includes('/auth/refresh') || url.includes('/auth/logout');

/**
 * HTTP interceptor that attaches the JWT to outgoing requests and reacts to
 * authentication errors by attempting a token refresh before falling back to
 * logging the user out and redirecting to login.
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
      const refreshToken = authService.getRefreshToken();

      // Only a 401 means the access token itself is missing/invalid/expired.
      // A 403 means the token is fine but the caller lacks permission for this
      // specific endpoint — that must never trigger a logout/refresh.
      if (err.status !== 401) {
        return throwError(() => err);
      }

      // 401 on a normal request with a refresh token available: try to
      // get a fresh access token and replay the original request once
      if (refreshToken && !isAuthEndpoint(req.url)) {
        return authService.refresh(refreshToken).pipe(
          switchMap(response => {
            authService.saveToken(response.token);
            authService.saveRefreshToken(response.refreshToken);
            const retryReq = req.clone({ headers: req.headers.append('Authorization', `Bearer ${response.token}`) });
            return next(retryReq);
          }),
          catchError(refreshErr => {
            // The refresh token itself is invalid/expired: the session is over
            authService.logout();
            router.navigate(['/login']);
            return throwError(() => refreshErr);
          })
        );
      }

      // No refresh token to try, or the failure came from an auth endpoint itself
      authService.logout();
      router.navigate(['/login']);

      // Re-throw so callers can still handle the error
      return throwError(() => err);
    })
  );
};
