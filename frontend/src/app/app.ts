import { Component, inject, signal, computed, effect, ChangeDetectorRef } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { MatIconModule } from "@angular/material/icon";
import { CommonModule, NgIf } from '@angular/common';
import { AuthService } from './core/services/AuthService';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSidenavModule } from '@angular/material/sidenav';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { UserService } from './core/services/UserService';
import { MatMenu, MatMenuModule, MatMenuTrigger } from "@angular/material/menu";

// Routes on which the app shell (sidenav, header) must be hidden
const AUTH_ROUTES = ['/login', '/register'];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ReactiveFormsModule,
    CommonModule, MatSidenavModule, MatIconModule, MatMenu, MatMenuModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
/**
 * Root component. Hosts the application shell (sidenav/header) and exposes the
 * current user's initials and role, hiding the shell on auth pages.
 */
export class App {
  protected readonly title = signal('frontend');
  private authService = inject(AuthService);
  private router = inject(Router);
  private userService = inject(UserService)

  // User's initials shown in the avatar
  initials = signal('');
  // Current user's role, loaded once authenticated
  role : string | null = ""
  // Whether a session token is present at startup
  isAuthenticated: boolean = this.authService.isAuthenticated();

  constructor(
    private cdr : ChangeDetectorRef
  ) {
    // React to navigation: once on a non-auth page, load the user's initials/role once
    effect(() => {
      const url = this.currentUrl();
      if (!AUTH_ROUTES.includes(url) && !this.initials()) {
        this.loadInitials();

        this.role = this.authService.getRole();
      }
    });
  }

  // Signal tracking the current URL, updated after each completed navigation
  private currentUrl = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(e => (e as NavigationEnd).urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

  // Show the app shell everywhere except on the auth pages
  showShell = computed(() => !AUTH_ROUTES.includes(this.currentUrl()));

  /**
   * Loads the authenticated user and derives their two-letter initials.
   */
  private loadInitials(): void {
    // Nothing to load without a known user id
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.userService.getUserById(userId).subscribe({
      next: (user) => {
        // Take the first letter of up to two name parts, uppercased
        const initials = user.name.split(' ').map((n: string) => n[0]).join('').toUpperCase().slice(0, 2);
        this.initials.set(initials);

        // Force a change-detection pass since this runs outside Angular's flow
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Logs the user out, clears the avatar initials and returns to login.
   */
  logout(): void {
    this.authService.logout();

    this.initials.set('');
    this.router.navigate(['/login']);
  }
}
