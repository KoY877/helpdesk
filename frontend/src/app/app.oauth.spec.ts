import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router } from '@angular/router';
import { Subject, of } from 'rxjs';
import { vi } from 'vitest';
import { App } from './app';
import { AuthService } from './core/services/AuthService';
import { UserService } from './core/services/UserService';

describe('OAuth callback in the app shell', () => {
  it('waits for authentication, then loads the profile on dashboard navigation', async () => {
    const events = new Subject<NavigationEnd>();
    const router = { url: '/', events, navigate: vi.fn() };
    const auth = {
      isAuthenticated: vi.fn().mockReturnValue(false),
      getUserId: vi.fn().mockReturnValue(null), getRole: vi.fn().mockReturnValue('USER'),
      logout: vi.fn(),
    };
    const users = { getUserById: vi.fn().mockReturnValue(of({ name: 'GitHub User' })) };
    TestBed.configureTestingModule({ imports: [App], providers: [
      { provide: Router, useValue: router },
      { provide: AuthService, useValue: auth },
      { provide: UserService, useValue: users },
    ] }).overrideComponent(App, { set: { template: '', imports: [] } });
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    events.next(new NavigationEnd(1, '/oauth/callback#code=test', '/oauth/callback#code=test'));
    fixture.detectChanges();
    expect(fixture.componentInstance.showShell()).toBe(false);
    expect(router.navigate).not.toHaveBeenCalled();
    expect(users.getUserById).not.toHaveBeenCalled();
    auth.isAuthenticated.mockReturnValue(true);
    auth.getUserId.mockReturnValue('user-id');
    events.next(new NavigationEnd(2, '/dashboard', '/dashboard'));
    fixture.detectChanges();
    expect(users.getUserById).toHaveBeenCalledWith('user-id');
    expect(fixture.componentInstance.showShell()).toBe(true);
  });
});
