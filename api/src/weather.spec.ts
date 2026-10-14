import { wmoCode } from './weather';

describe('wmoCode', () => {
  it('turns the sky of Foreca into the WMO codes the app reads', () => {
    expect(wmoCode('d000', 0)).toBe(0);
    expect(wmoCode('n100', 0)).toBe(1);
    expect(wmoCode('d200', 0)).toBe(2);
    expect(wmoCode('d400', 0)).toBe(3);
    expect(wmoCode('d600', 0)).toBe(45);
  });

  it('keeps the kind and the strength of what falls', () => {
    expect(wmoCode('d410', 0.2)).toBe(61);
    expect(wmoCode('d420', 0.35)).toBe(80);
    expect(wmoCode('d430', 2)).toBe(63);
    expect(wmoCode('d431', 2)).toBe(67);
    expect(wmoCode('d422', 1)).toBe(85);
    expect(wmoCode('d432', 1)).toBe(73);
  });

  it('makes heavy rain a downpour, and a storm stays a storm however heavy', () => {
    expect(wmoCode('d430', 8)).toBe(65);
    expect(wmoCode('d420', 8)).toBe(82);
    expect(wmoCode('d440', 20)).toBe(95);
  });
});
