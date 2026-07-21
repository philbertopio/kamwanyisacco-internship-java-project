package com.kimwanyisacco.web;

import com.kimwanyisacco.repository.AdminRepository;
import com.kimwanyisacco.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("currentUser")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CurrentUserContext {

    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;

    private Long memberId;
    private Long adminId;
    private String fullName;
    private boolean resolved = false;

    @Autowired
    public CurrentUserContext(MemberRepository memberRepository, AdminRepository adminRepository) {
        this.memberRepository = memberRepository;
        this.adminRepository = adminRepository;
    }

    public String getUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
    }

    public boolean isAdmin() {
        resolveIfNeeded();
        return adminId != null;
    }

    public Long getMemberId() {
        resolveIfNeeded();
        return memberId;
    }

    public Long getAdminId() {
        resolveIfNeeded();
        return adminId;
    }

    public String getFullName() {
        resolveIfNeeded();
        return fullName;
    }

    private void resolveIfNeeded() {
        if (resolved) {
            return;
        }
        String username = getUsername();
        if (username != null) {
            memberRepository.findByUserUsername(username).ifPresent(m -> {
                this.memberId = m.getId();
                this.fullName = m.getFullName();
            });
            adminRepository.findByUserUsername(username).ifPresent(a -> {
                this.adminId = a.getId();
                this.fullName = a.getFullName();
            });
        }
        resolved = true;
    }
}