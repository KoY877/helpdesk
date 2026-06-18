import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DashboardComponent } from './dashboard';

// Smoke test suite for the Dashboard component
describe('Dashboard', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    // Compile the standalone component
    await TestBed.configureTestingModule({
      imports: [DashboardComponent]
    })
    .compileComponents();

    // Create the component and wait for it to stabilize
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  // Verifies the component instantiates
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
