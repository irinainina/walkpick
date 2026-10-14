import { Controller, Get, HttpCode, Post } from '@nestjs/common';
import { Account, Accounts } from './accounts';
import { Reads, UserId } from './auth/auth.guard';
import { Guarded } from './docs';
import { Export } from './export';

@Guarded('me')
@Controller('v1/me')
export class MeController {
  constructor(
    private readonly accounts: Accounts,
    private readonly exported: Export,
  ) {}

  @Get()
  me(@UserId() id: string): Promise<Account> {
    return this.accounts.find(id);
  }

  /** Export my data: a POST, as the file is made anew each time. */
  @Post('export')
  @HttpCode(200)
  @Reads()
  export(@UserId() id: string) {
    return this.exported.of(id);
  }
}
