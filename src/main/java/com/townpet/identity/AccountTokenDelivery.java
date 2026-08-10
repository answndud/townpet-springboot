package com.townpet.identity;

interface AccountTokenDelivery {
  void deliver(AccountTokenPurpose purpose, String recipient, String rawToken);
}

enum AccountTokenPurpose {
  PASSWORD_RESET,
  EMAIL_VERIFICATION
}
