package com.Practice.URL.security;

import com.Practice.URL.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public String email() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof String email)
                || email.isBlank()) {
            throw new ForbiddenException("No authenticated user");
        }

        return email;
    }
}
