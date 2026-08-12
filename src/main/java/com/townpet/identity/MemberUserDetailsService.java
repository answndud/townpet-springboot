package com.townpet.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberUserDetailsService implements UserDetailsService {
  private final CredentialRepository credentials;
  private final boolean demoDataEnabled;

  public MemberUserDetailsService(
      CredentialRepository credentials,
      @Value("${townpet.demo-data.enabled:false}") boolean demoDataEnabled) {
    this.credentials = credentials;
    this.demoDataEnabled = demoDataEnabled;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    CredentialEntity credential =
        credentials
            .findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    if (!demoDataEnabled
        && username.toLowerCase(java.util.Locale.ROOT).endsWith("@townpet.local")) {
      throw new UsernameNotFoundException("Demo credentials are disabled");
    }
    return User.withUsername(credential.getMemberId().toString())
        .password(credential.getPasswordHash())
        .disabled(!credential.isEnabled() || !credential.isEmailVerified())
        .roles(credential.getRole())
        .build();
  }
}
