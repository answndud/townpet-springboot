package com.townpet.engagement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.townpet.common.ClientAddress;
import com.townpet.common.RequestRateLimiter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class GuestCommentRateLimitTest {
  @Test
  void limitsGuestCommentCreationHourly() {
    CommentService comments = mock(CommentService.class);
    when(comments.createGuest(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(mock(CommentEntity.class));
    GuestCommentController controller =
        new GuestCommentController(comments, new RequestRateLimiter(), new ClientAddress(""));
    UUID guest = UUID.randomUUID();
    UUID publication = UUID.randomUUID();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("198.51.100.11");
    GuestCommentController.CreateRequest body =
        new GuestCommentController.CreateRequest("guest-password", "comment", null);

    for (int index = 0; index < 30; index++) {
      controller.create(publication, guest, body, request);
    }
    assertThrows(
        ResponseStatusException.class, () -> controller.create(publication, guest, body, request));
  }
}
