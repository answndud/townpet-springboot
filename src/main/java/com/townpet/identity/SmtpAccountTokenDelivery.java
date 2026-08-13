package com.townpet.identity;

import java.net.URI;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("production | smtp-local")
@ConditionalOnProperty(name = "townpet.email.enabled", havingValue = "true")
final class SmtpAccountTokenDelivery implements AccountTokenDelivery {
  private final JavaMailSender mailSender;
  private final String from;
  private final URI publicBaseUrl;

  SmtpAccountTokenDelivery(JavaMailSender mailSender, TownpetEmailProperties properties) {
    this.mailSender = mailSender;
    this.from = properties.from();
    this.publicBaseUrl = properties.publicBaseUrl();
  }

  @Override
  public void deliver(AccountTokenPurpose purpose, String recipient, String rawToken) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(recipient);
    message.setSubject(subject(purpose));
    message.setText(body(purpose, rawToken));
    mailSender.send(message);
  }

  private String subject(AccountTokenPurpose purpose) {
    return switch (purpose) {
      case PASSWORD_RESET -> "TownPet 비밀번호 재설정";
      case EMAIL_VERIFICATION -> "TownPet 이메일 인증";
    };
  }

  private String body(AccountTokenPurpose purpose, String rawToken) {
    String path =
        switch (purpose) {
          case PASSWORD_RESET -> "/password/reset?token=";
          case EMAIL_VERIFICATION -> "/verify-email?token=";
        };
    return "TownPet 요청을 확인하려면 아래 링크를 이용하세요.\n\n"
        + publicBaseUrl.resolve(path + rawToken)
        + "\n\n본인이 요청하지 않았다면 이 메일을 무시하세요. 링크는 1시간 동안만 유효합니다.";
  }
}

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "townpet.email")
record TownpetEmailProperties(
    boolean enabled, String from, URI publicBaseUrl, String tokenEncryptionKey) {
  TownpetEmailProperties {
    Objects.requireNonNull(from, "townpet.email.from");
    Objects.requireNonNull(publicBaseUrl, "townpet.email.public-base-url");
    Objects.requireNonNull(tokenEncryptionKey, "townpet.email.token-encryption-key");
  }
}
