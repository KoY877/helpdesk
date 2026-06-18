import { TestBed } from '@angular/core/testing';

import { TicketService } from './TicketService';

// Smoke test suite for TicketService
describe('TicketService', () => {
  let service: TicketService;

  beforeEach(() => {
    // Spin up the testing module and resolve the service under test
    TestBed.configureTestingModule({});
    service = TestBed.inject(TicketService);
  });

  // Verifies the service can be instantiated by Angular's DI
  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
