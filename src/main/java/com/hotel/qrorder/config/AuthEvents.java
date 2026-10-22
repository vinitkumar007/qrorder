package com.hotel.qrorder.config;

import com.hotel.qrorder.service.LoginAttempts;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEvents {
    private final LoginAttempts attempts;

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent e) {
        attempts.fail(e.getAuthentication().getName());
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent e) {
        attempts.reset(e.getAuthentication().getName());
    }
}
