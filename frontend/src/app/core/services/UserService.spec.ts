import { TestBed } from '@angular/core/testing';

import { UserService } from './UserService';

// Smoke test suite for UserService
describe('UserService', () => {
  let service: UserService;

  beforeEach(() => {
    // Spin up the testing module and resolve the service under test
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserService);
  });

  // Verifies the service can be instantiated by Angular's DI
  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
