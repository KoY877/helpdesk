import { CamelcasePipe } from './camelcase-pipe';

// Smoke test suite for CamelcasePipe
describe('CamelcasePipe', () => {
  // Verifies the pipe can be instantiated
  it('create an instance', () => {
    const pipe = new CamelcasePipe();
    expect(pipe).toBeTruthy();
  });
});
