package com.kimwanyisacco.web;

import com.kimwanyisacco.dto.request.MemberRegistrationRequest;
import com.kimwanyisacco.exception.DuplicateResourceException;
import com.kimwanyisacco.service.MemberService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

@Getter
@Setter
@Component("registrationBean")
@Scope("request")
public class RegistrationBean {

    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String fullName;
    private String nationalId;
    private String phone;
    private String address;

    private final MemberService memberService;

    @Autowired
    public RegistrationBean(MemberService memberService) {
        this.memberService = memberService;
    }

    public String register() {

        System.out.println(">>> register() called with username=" + username);
        FacesContext context = FacesContext.getCurrentInstance();

        if (password != null && !password.equals(confirmPassword)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Passwords do not match", null));
            return null;
        }

        try {
            MemberRegistrationRequest request = new MemberRegistrationRequest(
                    username, email, password, fullName, nationalId, phone, address);
            memberService.registerMember(request);

            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Registration successful. Please log in.", null));
            return "login?faces-redirect=true";

        } catch (DuplicateResourceException ex) {
            ex.printStackTrace();
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        } catch (Exception ex) {
            ex.printStackTrace(); // TEMP: show us what's actually breaking
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Registration failed: " + ex.getMessage(), null)); // TEMP: surface real message to page too
            return null;
        }
    }
}

