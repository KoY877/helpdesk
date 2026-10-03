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
    const params = new URLSearchParams(this.route.snapshot.fragment ?? '');
    window.history.replaceState(null, '', window.location.pathname);
    const code = params.get('code');
    if (!code || params.has('error')) {
      this.message = params.get('error') === 'account_link_required'
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
