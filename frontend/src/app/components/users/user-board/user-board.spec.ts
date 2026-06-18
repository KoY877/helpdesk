import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserBoardComponent } from './user-board';

// Smoke test suite for the UserBoard component
describe('UserBoard', () => {
  let component: UserBoardComponent;
  let fixture: ComponentFixture<UserBoardComponent>;

  beforeEach(async () => {
    // Compile the standalone component
    await TestBed.configureTestingModule({
      imports: [UserBoardComponent]
    })
    .compileComponents();

    // Create the component and wait for it to stabilize
    fixture = TestBed.createComponent(UserBoardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  // Verifies the component instantiates
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
