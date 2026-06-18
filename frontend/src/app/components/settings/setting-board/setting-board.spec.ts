import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SettingBoard } from './setting-board';

// Smoke test suite for the SettingBoard component
// NOTE: imports 'SettingBoard' but the class is exported as 'SettingBoardComponent' — broken import
describe('SettingBoard', () => {
  let component: SettingBoard;
  let fixture: ComponentFixture<SettingBoard>;

  beforeEach(async () => {
    // Compile the standalone component
    await TestBed.configureTestingModule({
      imports: [SettingBoard]
    })
    .compileComponents();

    // Create the component and wait for it to stabilize
    fixture = TestBed.createComponent(SettingBoard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  // Verifies the component instantiates
  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
