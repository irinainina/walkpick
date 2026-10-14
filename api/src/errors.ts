import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import { Response } from 'express';

type Shaped = {
  error: {
    code: string;
    message: string;
    fields?: { field: string; problem: string }[];
  };
};

const shaped = (body: unknown): body is Shaped =>
  typeof (body as Shaped | null)?.error?.code === 'string';

// What Nest and Express refuse by themselves: an unknown address, a body that is not JSON or too big.
const OWN: Partial<Record<number, Shaped>> = {
  [HttpStatus.BAD_REQUEST]: {
    error: {
      code: 'validation_failed',
      message: 'body is not_json',
      fields: [{ field: 'body', problem: 'not_json' }],
    },
  },
  [HttpStatus.NOT_FOUND]: {
    error: { code: 'not_found', message: 'there is no such address' },
  },
  [HttpStatus.PAYLOAD_TOO_LARGE]: {
    error: { code: 'too_large', message: 'the body is too large' },
  },
};

const INTERNAL: Shaped = {
  error: { code: 'internal', message: 'something went wrong' },
};

/** Gives every refusal the one body clients read; details of a failure stay in the log. */
@Catch()
export class ErrorShape implements ExceptionFilter {
  private readonly log = new Logger(ErrorShape.name);

  catch(problem: unknown, host: ArgumentsHost): void {
    const response = host.switchToHttp().getResponse<Response>();
    const known = problem instanceof HttpException;
    const status = known
      ? problem.getStatus()
      : ((problem as { status?: number } | null)?.status ?? 500);
    const body = known ? problem.getResponse() : undefined;
    const answer = shaped(body) ? body : OWN[status];
    if (answer) {
      response.status(status).json(answer);
      return;
    }
    this.log.error(problem);
    response.status(HttpStatus.INTERNAL_SERVER_ERROR).json(INTERNAL);
  }
}
