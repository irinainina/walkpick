import { Controller, Get } from '@nestjs/common';
import { Account, Accounts } from './accounts';
import { UserId } from './auth/auth.guard';

@Controller('me')
export class MeController {
  constructor(private readonly accounts: Accounts) {}

  @Get()
  me(@UserId() id: string): Promise<Account> {
    return this.accounts.find(id);
  }
}
