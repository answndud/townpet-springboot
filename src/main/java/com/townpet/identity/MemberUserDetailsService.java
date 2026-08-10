package com.townpet.identity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberUserDetailsService implements UserDetailsService {
  private final CredentialRepository credentials;

  public MemberUserDetailsService(CredentialRepository credentials) {
    this.credentials = credentials;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    CredentialEntity credential =
        credentials
            .findByEmailIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    return User.withUsername(credential.getMemberId().toString())
        .password(credential.getPasswordHash())
        .disabled(!credential.isEnabled())
        .roles("MEMBER")
        .build();
  }
}
