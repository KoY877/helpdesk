import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { authGuard } from './auth-guard';

// Smoke test suite for the authGuard route guard
describe('authGuard', () => {
  // Wrapper running the functional guard inside Angular's injection context
  const executeGuard: CanActivateFn = (...guardParameters) =>
      TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    // Minimal testing module is enough for this smoke test
    TestBed.configureTestingModule({});
  });

  // Verifies the guard function is defined
  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
