package com.lifelink.security;

import com.lifelink.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public UserPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("You must be logged in to perform this action");
    }

    public Long getCurrentUserId() {
        return getPrincipal().getId();
    }

    public boolean isAdmin() {
        return "ADMIN".equals(getPrincipal().getRole());
    }
}
