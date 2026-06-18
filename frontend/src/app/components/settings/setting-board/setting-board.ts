import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/AuthService';
import { UserService } from '../../../core/services/UserService';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';

@Component({
  selector: 'app-setting-board',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ReactiveFormsModule,
    CommonModule, MatSidenavModule, MatIconModule],
  templateUrl: './setting-board.html',
  styleUrl: './setting-board.scss',
})
/**
 * Settings shell hosting the nested profile/password tabs via a side navigation.
 */
export class SettingBoardComponent {
  protected readonly title = signal('frontend');
  private authService = inject(AuthService);
  private router = inject(Router);
  private userService = inject(UserService)

  // User's initials shown in the avatar
  initials = signal('');
  // Current user's role
  role = this.authService.getRole();
  // Whether a session token is present
  isAuthenticated: boolean = this.authService.isAuthenticated();

  constructor() {

  }

  // Signal tracking the current URL, updated after each completed navigation
  private currentUrl = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(e => (e as NavigationEnd).urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );


}
