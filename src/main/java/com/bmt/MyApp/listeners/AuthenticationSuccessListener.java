package com.bmt.MyApp.listeners;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.bmt.MyApp.services.LogService;

@Component
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final LogService logService;

    public AuthenticationSuccessListener(LogService logService) {
        this.logService = logService;
    }

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        logService.log(username, "Đăng nhập", "Đăng nhập thành công");
    }
}

