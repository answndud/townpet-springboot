package com.townpet.publication;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.townpet.common.ClientAddress;
import com.townpet.common.RequestRateLimiter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class GuestPublicationRateLimitTest {
  @Test
  void limitsGuestPostCreationHourlyButKeepsGuestsIndependent() {
    PublicationService publications = mock(PublicationService.class);
    when(publications.createGuest(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(mock(PublicationEntity.class));
    GuestPublicationController controller =
        new GuestPublicationController(
            publications, new RequestRateLimiter(), new ClientAddress(""));
    UUID firstGuest = UUID.randomUUID();
    UUID secondGuest = UUID.randomUUID();
    MockHttpServletRequest request = request("198.51.100.10");
    GuestPublicationController.CreateRequest body =
        new GuestPublicationController.CreateRequest("guest-password", "title", "body");

    for (int index = 0; index < 10; index++) {
      controller.create(firstGuest, body, request);
    }
    assertThrows(ResponseStatusException.class, () -> controller.create(firstGuest, body, request));
    controller.create(secondGuest, body, request);
  }

  private static MockHttpServletRequest request(String remoteAddress) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddress);
    return request;
  }
}
