import { shiftDay, spanOf } from './period';

describe('spanOf', () => {
  it('starts a week on Monday, also for a Sunday and across a month', () => {
    expect(spanOf('week', '2026-10-13')).toEqual({
      from: '2026-10-12',
      to: '2026-10-19',
      step: 'day',
    });
    expect(spanOf('week', '2026-11-01')).toEqual({
      from: '2026-10-26',
      to: '2026-11-02',
      step: 'day',
    });
  });

  it('takes a whole month and a whole year', () => {
    expect(spanOf('month', '2026-12-31')).toEqual({
      from: '2026-12-01',
      to: '2027-01-01',
      step: 'day',
    });
    expect(spanOf('year', '2026-02-10')).toEqual({
      from: '2026-01-01',
      to: '2027-01-01',
      step: 'month',
    });
  });

  it('has no bounds for all the time', () => {
    expect(spanOf('all', '2026-02-10')).toEqual({
      from: null,
      to: null,
      step: 'year',
    });
  });

  it('shifts a day over the end of a month', () => {
    expect(shiftDay('2026-03-01', -1)).toBe('2026-02-28');
  });
});
