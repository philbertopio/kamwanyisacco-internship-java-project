package com.kimwanyisacco.security;

import com.kimwanyisacco.model.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Adapts our User entity to Spring Security's UserDetails contract.
 * Also exposes the underlying domain id (Member id or Admin id is resolved
 * separately by the beans that need it) so JSF managed beans can look up
 * "who is logged in" without re-querying the DB for basic identity checks.
 */
@Getter
public class SaccoUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final String role; // "MEMBER" or "ADMIN"
    private final boolean active;

    public SaccoUserPrincipal(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().name();
        this.active = user.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security convention: prefix with ROLE_ for hasRole() checks to work.
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
