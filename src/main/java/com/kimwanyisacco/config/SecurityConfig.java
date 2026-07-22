package com.kimwanyisacco.config;


import com.kimwanyisacco.security.CustomUserDetailsService;
import com.kimwanyisacco.security.RoleBasedAuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;

/**
 * Central security policy for the whole application.
 *
 * Design choices worth noting:
 * - BCrypt for password hashing (already used at registration time in MemberServiceImpl).
 * - Session fixation protection: a new session ID is issued on login.
 * - Only one concurrent session per user, to reduce risk from a leaked/shared session cookie.
 * - CSRF protection is ON (default) - JSF forms must include the token, see template.xhtml.
 * - Method-level security (@PreAuthorize) is enabled so business rules like
 *   "only an admin may approve a loan" are enforced in the service layer itself,
 *   not just by hiding buttons in the UI.
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CustomUserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        auth.authenticationProvider(provider);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                // Public resources: static assets, landing/login/registration pages
                .antMatchers("/resources/**", "/index.xhtml", "/login.xhtml",
                             "/register.xhtml", "/javax.faces.resource/**").permitAll()
                .antMatchers("/api/v1/members").permitAll() // registration endpoint stays open
                // PesaPal IPN — no-auth: PesaPal cannot send a Bearer/session token.
                // Security: PesaPal only calls URLs registered through its API.
                .antMatchers("/api/v1/payments/ipn").permitAll()
                // Role-scoped areas - matched to the JSF folder structure
                .antMatchers("/admin/**", "/api/v1/loans/*/decision", "/api/v1/loans/overdue")
                    .hasRole("ADMIN")
                .antMatchers("/member/**").hasRole("MEMBER")
                .antMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login.xhtml")
                .loginProcessingUrl("/j_spring_security_check")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(new RoleBasedAuthenticationSuccessHandler())
                .failureUrl("/login.xhtml?error=true")
                .permitAll()
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.xhtml?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            .and()
            .sessionManagement()
                .sessionFixation().migrateSession() // new session id issued on login
                .maximumSessions(1)
                .expiredUrl("/login.xhtml?expired=true")
            .and()
            .and()
            .exceptionHandling()
                .accessDeniedPage("/accessDenied.xhtml")
            .and()
            // Disable CSRF for stateless REST API paths — they use session/cookie auth
            // but are called by non-browser clients (PesaPal IPN, mobile apps).
            // JSF pages still benefit from CSRF protection via Spring Security's default.
            .csrf()
                .ignoringAntMatchers("/api/v1/**")
            .and()
            .headers()
                .frameOptions().deny()
                .httpStrictTransportSecurity().disable(); // enable this once served over HTTPS
    }
}
