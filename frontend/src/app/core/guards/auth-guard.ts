import { inject, Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/AuthService';


/**
 * Route guard that only lets authenticated users through.
 * Unauthenticated users are redirected to the login page.
 *
 * @param route the route being activated
 * @param state the router state snapshot
 * @returns true if access is granted, or a UrlTree redirecting to /login
 */
export const authGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  // Resolve the dependencies via Angular's functional injection
  const authService = inject(AuthService);
  const router = inject(Router);

  // Allow navigation when a session token is present
  if (authService.isAuthenticated()) {
    return true;
  }

  // Otherwise redirect to the login page
  return router.createUrlTree(['/login']);
};
