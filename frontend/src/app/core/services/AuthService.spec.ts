import { TestBed } from '@angular/core/testing';

import { AuthService } from './AuthService';

// Smoke test suite for AuthService
describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    // Spin up the testing module and resolve the service under test
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

  // Verifies the service can be instantiated by Angular's DI
  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
