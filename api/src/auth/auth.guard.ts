import {
  CanActivate,
  createParamDecorator,
  ExecutionContext,
  Injectable,
  Logger,
  SetMetadata,
  UnauthorizedException,
} from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { Request } from 'express';
import { SigningKeys } from './signing-keys';
import { keyIdOf, TokenError, userOf } from './token';

const PUBLIC = 'public';

/** Marks a request that is answered without a token. */
export const Public = () => SetMetadata(PUBLIC, true);

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
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    if (
      this.reflector.getAllAndOverride(PUBLIC, [
        context.getHandler(),
        context.getClass(),
      ])
    ) {
      return true;
    }
    const request = context.switchToHttp().getRequest<SignedRequest>();
    const [scheme, token] = (request.headers.authorization ?? '').split(' ');
    try {
      if (scheme !== 'Bearer' || !token) throw new TokenError('no token');
      const key = await this.keys.find(keyIdOf(token));
      request.userId = userOf(token, key, this.keys.issuer, new Date());
      return true;
    } catch (problem) {
      if (!(problem instanceof TokenError)) throw problem;
      this.log.warn(problem.message);
      throw new UnauthorizedException({
        error: { code: 'unauthorized', message: 'a valid token is required' },
      });
    }
  }
}
