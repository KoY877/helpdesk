import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { App } from './app';
import { OAuthCallbackComponent } from './components/login/oauth-callback';
import { UserService } from './core/services/UserService';
import { AuthService } from './core/services/AuthService';
import { authGuard } from './core/guards/auth-guard';

@Component({ standalone: true, template: 'Dashboard ready' })
class TestDashboard {}

describe('GitHub callback with the real app layout', () => {
  it('exchanges the code once and displays the protected dashboard', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [App], providers: [
      provideRouter([
        { path: 'oauth/callback', component: OAuthCallbackComponent },
        { path: 'dashboard', component: TestDashboard, canActivate: [authGuard] },
      ]),
      provideHttpClient(), provideHttpClientTesting(),
      { provide: UserService, useValue: { getUserById: () => of({ name: 'GitHub User' }) } },
    ] });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/oauth/callback#code=one-time-code');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const exchange = http.expectOne(request => request.url.endsWith('/auth/oauth/exchange'));
    expect(exchange.request.body).toEqual({ code: 'one-time-code' });
    expect(exchange.request.withCredentials).toBe(true);
    exchange.flush({ token: 'access', refreshToken: 'refresh', role: 'USER', userId: 'local-user' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(router.url).toBe('/dashboard');
    expect(TestBed.inject(AuthService).getToken()).toBe('access');
    expect(fixture.nativeElement.textContent).toContain('Dashboard ready');
    http.verify();
    localStorage.clear();
  });

  it('accepts OAuth codes coming from the query string as well as the hash fragment', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [App], providers: [
      provideRouter([
        { path: 'oauth/callback', component: OAuthCallbackComponent },
        { path: 'dashboard', component: TestDashboard, canActivate: [authGuard] },
      ]),
      provideHttpClient(), provideHttpClientTesting(),
      { provide: UserService, useValue: { getUserById: () => of({ name: 'GitHub User' }) } },
    ] });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/oauth/callback?code=query-code');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const exchange = http.expectOne(request => request.url.endsWith('/auth/oauth/exchange'));
    expect(exchange.request.body).toEqual({ code: 'query-code' });
    exchange.flush({ token: 'access', refreshToken: 'refresh', role: 'USER', userId: 'local-user' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(router.url).toBe('/dashboard');
    http.verify();
    localStorage.clear();
  });
});
