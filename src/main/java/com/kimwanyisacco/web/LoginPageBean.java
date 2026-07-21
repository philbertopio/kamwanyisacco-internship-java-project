package com.kimwanyisacco.web;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.faces.context.FacesContext;

/**
 * Spring Security redirects back to the login page with ?error=true,
 * ?logout=true, or ?expired=true. This bean turns those into readable
 * banners on the page instead of a raw query string.
 */
@Component("loginPageBean")
@Scope("request")
public class LoginPageBean {

    public boolean isError() {
        return hasParam("error");
    }

    public boolean isLoggedOut() {
        return hasParam("logout");
    }

    public boolean isSessionExpired() {
        return hasParam("expired");
    }

    private boolean hasParam(String name) {
        return FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap()
                .containsKey(name);
    }
}
