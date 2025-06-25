package com.bmt.MyApp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/register", "/register/**", "/login", "/verify-otp", "/verify-otp/**", 
                               "/resend-otp", "/css/**", "/js/**", "/image/**").permitAll()
                .requestMatchers("/home", "/profile").hasAnyRole("ADMIN", "CLIENT", "ADMINDICHVU")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/account_management", "/lichsugiaodich").hasRole("ADMIN")
                .requestMatchers("/services").hasAnyRole("CLIENT", "ADMINDICHVU")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true"))
            .logout(config -> config
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}