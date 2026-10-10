import { Component, OnInit, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/AuthService';
import { AuthResponse } from '../../core/models/auth.model';
import { environement } from '../../core/environements/environements';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [RouterLink],
  template: `<p role="status">{{ message }}</p><a routerLink="/login">Back to login</a>`,
})
export class OAuthCallbackComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  message = 'Signing in with GitHub…';

  ngOnInit(): void {
    const fragmentParams = new URLSearchParams(this.route.snapshot.fragment ?? '');
    const queryParams = this.route.snapshot.queryParamMap;
    const code = fragmentParams.get('code') ?? queryParams.get('code');
    const error = fragmentParams.get('error') ?? queryParams.get('error');
    window.history.replaceState(null, '', window.location.pathname);
    if (!code || error) {
      this.message = error === 'account_link_required'
        ? 'An account already uses this email. Please sign in with your password.'
        : 'GitHub sign-in failed. Please try again.';
      return;
    }
    this.http.post<AuthResponse>(`${environement.apiUrl}/auth/oauth/exchange`, { code },
      { withCredentials: true }).subscribe({
      next: (response) => {
        this.auth.saveToken(response.token);
        this.auth.saveRefreshToken(response.refreshToken);
        this.auth.saveRole(response.role);
        this.auth.saveUserId(response.userId);
        this.router.navigateByUrl('/dashboard', { replaceUrl: true });
      },
      error: () => { this.message = 'GitHub sign-in expired or failed. Please try again.'; },
    });
  }
}
