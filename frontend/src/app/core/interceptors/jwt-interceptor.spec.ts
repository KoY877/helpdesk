import { TestBed } from '@angular/core/testing';
import { HttpInterceptorFn } from '@angular/common/http';

import { jwtInterceptor } from './jwt-interceptor';

// Smoke test suite for the jwtInterceptor HTTP interceptor
describe('jwtInterceptor', () => {
  // Wrapper running the functional interceptor inside Angular's injection context
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => jwtInterceptor(req, next));

  beforeEach(() => {
    // Minimal testing module is enough for this smoke test
    TestBed.configureTestingModule({});
  });

  // Verifies the interceptor function is defined
  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });
});
