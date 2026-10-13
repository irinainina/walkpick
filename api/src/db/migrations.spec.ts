import { pending, upTo } from './migrations';

const files = [
  '0002_policies.sql',
  '0001_schema.sql',
  'notes.txt',
  '0003_photos.sql',
];

describe('migrations', () => {
  it('lists what is not applied yet, in the order of the numbers', () => {
    expect(pending(files, ['0001_schema.sql'])).toEqual([
      '0002_policies.sql',
      '0003_photos.sql',
    ]);
    expect(pending(files, files)).toEqual([]);
  });

  it('takes the migrations up to a number as already applied', () => {
    expect(upTo(files, '0002')).toEqual([
      '0001_schema.sql',
      '0002_policies.sql',
    ]);
  });
});
