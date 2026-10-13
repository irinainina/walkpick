import {
  amount,
  change,
  choice,
  fields,
  flag,
  listOf,
  orNull,
  text,
  uuid,
} from './body';

const problem = (run: () => unknown) => {
  try {
    run();
  } catch (refused) {
    return (
      refused as { getResponse(): { error: { message: string } } }
    ).getResponse().error.message;
  }
  return 'accepted';
};

describe('fields', () => {
  const spec = {
    name: text(5),
    is_best: flag,
    kind: choice(['plant', 'bird']),
    value: amount(10),
  };

  it('reads the fields that came and leaves the others out', () => {
    expect(
      fields({ name: ' yarrow '.slice(0, 6), is_best: true }, spec),
    ).toEqual({ name: 'yarro', is_best: true });
    expect(fields({ name: '   ' }, spec)).toEqual({ name: null });
    expect(fields({}, spec)).toEqual({});
  });

  it('refuses a field nobody expects, a wrong type, a long text and a body that is not an object', () => {
    expect(problem(() => fields({ user_id: 'x' }, spec))).toBe(
      'user_id is unknown',
    );
    expect(problem(() => fields({ is_best: 'yes' }, spec))).toBe(
      'is_best is not_a_flag',
    );
    expect(problem(() => fields({ name: 'too long' }, spec))).toBe(
      'name is too_long',
    );
    expect(problem(() => fields({ kind: 'fish' }, spec))).toBe(
      'kind is not_allowed',
    );
    expect(problem(() => fields({ value: 0 }, spec))).toBe(
      'value is out_of_range',
    );
    expect(problem(() => fields([], spec))).toBe('body is not_an_object');
  });

  it('checks every item of a list and lets null through where it is allowed', () => {
    const ids = { ids: listOf(uuid, 2), diastolic: orNull(amount(300)) };
    const one = '00000000-0000-4000-8000-000000000001';
    expect(fields({ ids: [one], diastolic: null }, ids)).toEqual({
      ids: [one],
      diastolic: null,
    });
    expect(problem(() => fields({ ids: [] }, ids))).toBe('ids is out_of_range');
    expect(problem(() => fields({ ids: [one, 'x'] }, ids))).toBe(
      'ids is not_an_id',
    );
  });

  it('refuses a change without fields', () => {
    expect(problem(() => change({}))).toBe('body is empty');
    expect(change({ name: null })).toEqual({ name: null });
  });
});
