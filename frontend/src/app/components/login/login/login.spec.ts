import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoginComponent } from './login';

// Smoke test suite for the Login component
describe('Login', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  beforeEach(async () => {
    // Compile the standalone component
    await TestBed.configureTestingModule({
      imports: [LoginComponent]
    })
    .compileComponents();

    // Create the component and wait for it to stabilize
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  // Verifies the component instantiates
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
