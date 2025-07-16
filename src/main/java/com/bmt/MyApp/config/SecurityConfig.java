package com.bmt.MyApp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.bmt.MyApp.handlers.CustomLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/register", "/register/**", "/login", "/verify-otp", "/verify-otp/**", 
                               "/resend-otp", "/css/**", "/js/**", "/image/**",
                               "/forgot-password", "/reset-password", "/reset-password/**").permitAll()
                .requestMatchers("/home", "/profile").hasAnyRole("ADMIN", "CLIENT", "ADMINDICHVU")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/account_management", "/lichsugiaodich").hasRole("ADMIN")
                .requestMatchers("/services", "/user_transactions", "/recharge").hasAnyRole("CLIENT", "ADMINDICHVU")
                .requestMatchers("/servicepacks/add-member", "/servicepacks/add-member-page", "/servicepacks/members/**").hasAnyRole("ADMIN", "ADMINDICHVU")
                .requestMatchers("/servicepacks/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true"))
            .logout(config -> config
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
