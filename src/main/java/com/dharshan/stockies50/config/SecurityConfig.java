package com.dharshan.stockies50.config;

import com.dharshan.stockies50.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /*@Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .csrf().disable()
          .authorizeHttpRequests(authorize -> authorize
              .requestMatchers("/", "/login", "/signup", "/about/**", "/css/**", "/js/**").permitAll()
              .anyRequest().authenticated()
          )
          .formLogin(form -> form
              .loginPage("/login")
              .defaultSuccessUrl("/results", true)
              .permitAll()
          )
          .logout(logout -> logout.permitAll());
        return http.build();
    }*/
    //// filepath: /D:/ADX_APPLICATION_CO-PILOT/stockies50/stockies50/src/main/java/com/dharshan/stockies50/config/SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf().disable()
      .authorizeHttpRequests(authorize -> authorize
          .requestMatchers(
              "/", 
              "/login", 
              "/signup", 
              "/about/**",
              "/about-stock-market/**", 
              "/about-nse-nifty50/**", 
              "/about-technical-indicators/**", 
              "/css/**", 
              "/js/**", 
              "/images/**"    // Allow images folder for favicon/bg images
          ).permitAll()
          .anyRequest().authenticated()
      )
      .formLogin(form -> form
          .loginPage("/login")
          .defaultSuccessUrl("/home", true)
          .permitAll()
      )
      .logout(logout -> logout.permitAll());
    return http.build();
}
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService)
           .passwordEncoder(passwordEncoder);
    }
}
