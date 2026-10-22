package com.hotel.qrorder.config;

import com.hotel.qrorder.repo.StaffUserRepo;
import com.hotel.qrorder.service.LoginAttempts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(StaffUserRepo repo, LoginAttempts attempts) {
        return username -> repo.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole())
                        .accountLocked(attempts.isLocked(u.getUsername()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid login"));
    }

    @Bean
    AuthenticationSuccessHandler successHandler() {
        return (request, response, auth) -> {
            boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            boolean waiter = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WAITER"));
            response.sendRedirect(admin ? "/admin.html" : waiter ? "/waiter.html" : "/kitchen.html");
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler) throws Exception {
        http
            .csrf(c -> c
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                // customer endpoints QR token se protected hain (login nahi hota)
                .ignoringRequestMatchers("/api/public/**"))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/t/**", "/api/public/**", "/customer.html", "/login.html",
                        "/css/**", "/js/**", "/uploads/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/kitchen.html", "/api/kitchen/**").hasAnyRole("KITCHEN", "WAITER", "ADMIN")
                .requestMatchers("/waiter.html", "/receipt.html", "/api/billing/**").hasAnyRole("WAITER", "ADMIN")
                .requestMatchers("/admin.html", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/account/**").authenticated()
                .anyRequest().denyAll())
            .formLogin(f -> f
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureUrl("/login.html?error")
                .permitAll())
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login.html"))
            .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()))
            .headers(h -> h
                .contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; "
                  + "script-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}
