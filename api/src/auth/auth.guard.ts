import {
  CanActivate,
  createParamDecorator,
  ExecutionContext,
  ForbiddenException,
  Injectable,
  Logger,
  SetMetadata,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Request } from 'express';
import { Settings } from '../config';
import { SigningKeys } from './signing-keys';
import { keyIdOf, TokenError, userOf } from './token';

const PUBLIC = 'public';
const READS = 'reads';

/** Marks a request that is answered without a token. */
export const Public = () => SetMetadata(PUBLIC, true);

/** Marks a request that is not a GET and still changes nothing: the demo account may make it. */
export const Reads = () => SetMetadata(READS, true);

type SignedRequest = Request & { userId?: string };

/** The user of the request, as the guard read it from the token. */
export const UserId = createParamDecorator(
  (_: unknown, context: ExecutionContext): string => {
    const { userId } = context.switchToHttp().getRequest<SignedRequest>();
    if (!userId)
      throw new Error('UserId is asked on a request the guard did not check');
    return userId;
  },
);

/** Lets a request through only with a valid Supabase token; the user comes from nowhere else. */
@Injectable()
export class AuthGuard implements CanActivate {
  private readonly log = new Logger(AuthGuard.name);

  constructor(
    private readonly reflector: Reflector,
    private readonly keys: SigningKeys,
    private readonly settings: Settings,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const marked = (key: string): boolean =>
      this.reflector.getAllAndOverride(key, [
        context.getHandler(),
        context.getClass(),
      ]) === true;
    if (marked(PUBLIC)) return true;
    const request = context.switchToHttp().getRequest<SignedRequest>();
    const [scheme, token] = (request.headers.authorization ?? '').split(' ');
    try {
      if (scheme !== 'Bearer' || !token) throw new TokenError('no token');
      const key = await this.keys.find(keyIdOf(token));
      request.userId = userOf(token, key, this.keys.issuer, new Date());
    } catch (problem) {
      if (!(problem instanceof TokenError)) throw problem;
      this.log.warn(problem.message);
      throw new UnauthorizedException({
        error: { code: 'unauthorized', message: 'a valid token is required' },
      });
    }
    const demo = this.settings.demo;
    if (
      request.userId === demo?.userId &&
      request.method !== 'GET' &&
      !marked(READS)
    )
      throw new ForbiddenException({
        error: {
          code: 'demo_read_only',
          message: 'the demo account is read-only',
        },
      });
    return true;
  }
}
