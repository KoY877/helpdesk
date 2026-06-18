import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TicketDialog } from './ticket-dialog';

// Smoke test suite for the TicketDialog component
// NOTE: imports 'TicketDialog' but the class is exported as 'TicketDialogComponent' — broken import
describe('TicketDialog', () => {
  let component: TicketDialog;
  let fixture: ComponentFixture<TicketDialog>;

  beforeEach(async () => {
    // Compile the standalone component
    await TestBed.configureTestingModule({
      imports: [TicketDialog]
    })
    .compileComponents();

    // Create the component and wait for it to stabilize
    fixture = TestBed.createComponent(TicketDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  // Verifies the component instantiates
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
