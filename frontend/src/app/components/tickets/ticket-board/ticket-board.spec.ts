import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { TicketBoardComponent } from './ticket-board';

// Smoke test suite for the TicketBoard component
describe('TicketBoard', () => {
  let component: TicketBoardComponent;
  let fixture: ComponentFixture<TicketBoardComponent>;

  beforeEach(async () => {
    // Compile the standalone component
    await TestBed.configureTestingModule({
      imports: [TicketBoardComponent],
    })
    .compileComponents();

    // Create the component and wait for it to stabilize
    fixture = TestBed.createComponent(TicketBoardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  // Verifies the component instantiates
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
