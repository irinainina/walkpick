import { Controller, Get } from '@nestjs/common';
import { UserId } from './auth/auth.guard';

@Controller('me')
export class MeController {
  @Get()
  me(@UserId() id: string): { id: string } {
    return { id };
  }
}
