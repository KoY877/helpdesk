import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserDialog } from './user-dialog';

// Smoke test suite for the UserDialog component
// NOTE: imports 'UserDialog' but the class is exported as 'UserDialogComponent' — broken import
describe('UserDialog', () => {
  let component: UserDialog;
  let fixture: ComponentFixture<UserDialog>;

  beforeEach(async () => {
    // Compile the standalone component
    await TestBed.configureTestingModule({
      imports: [UserDialog]
    })
    .compileComponents();

    // Create the component and wait for it to stabilize
    fixture = TestBed.createComponent(UserDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  // Verifies the component instantiates
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
