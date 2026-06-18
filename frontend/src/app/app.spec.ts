import { TestBed } from '@angular/core/testing';
import { App } from './app';

// Test suite for the root App component
describe('App', () => {
  beforeEach(async () => {
    // Compile the standalone root component before each test
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  // Verifies the root component instantiates
  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  // Verifies the rendered template shows the expected title
  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    // Wait for the template and signals to settle before reading the DOM
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Hello, frontend');
  });
});
