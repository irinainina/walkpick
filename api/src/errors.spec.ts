import { ArgumentsHost, Logger } from '@nestjs/common';
import { ErrorShape } from './errors';

describe('ErrorShape', () => {
  it('answers a failure of the service with 500 and keeps the details in the log', () => {
    const response = { status: jest.fn(), json: jest.fn() };
    response.status.mockReturnValue(response);
    const host = {
      switchToHttp: () => ({ getResponse: () => response }),
    } as unknown as ArgumentsHost;
    const logged = jest.spyOn(Logger.prototype, 'error').mockImplementation();
    new ErrorShape().catch(new Error('the database is gone'), host);
    expect(response.status).toHaveBeenCalledWith(500);
    expect(response.json).toHaveBeenCalledWith({
      error: { code: 'internal', message: 'something went wrong' },
    });
    expect(logged).toHaveBeenCalled();
  });
});
