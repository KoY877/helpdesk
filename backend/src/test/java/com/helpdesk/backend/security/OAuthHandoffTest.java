package com.helpdesk.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import com.helpdesk.backend.dto.AuthResponse;
import static org.junit.jupiter.api.Assertions.*;

class OAuthHandoffTest {
    @Test
    void exchangeRequiresMatchingSessionAndCodeAndIsSingleUse() {
        OAuthHandoff handoff = new OAuthHandoff();
        MockHttpSession session = new MockHttpSession();
        AuthResponse tokens = new AuthResponse("access", "refresh", "USER", "user-id");
        String code = handoff.store(session, tokens);
        assertThrows(BadCredentialsException.class, () -> handoff.exchange(null, code));
        assertThrows(BadCredentialsException.class, () -> handoff.exchange(new MockHttpSession(), code));
        assertThrows(BadCredentialsException.class, () -> handoff.exchange(session, "wrong"));
        assertEquals(tokens, handoff.exchange(session, code));
        assertTrue(session.isInvalid());
    }
}
