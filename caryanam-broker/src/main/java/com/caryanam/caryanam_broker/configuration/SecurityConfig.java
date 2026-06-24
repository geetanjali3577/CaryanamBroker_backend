//package com.caryanam.caryanam_broker.configuration;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.*;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//public class SecurityConfig {
//
//    @Autowired
//    private JwtFilter jwtFilter;
//
//    @Autowired
//    private CustomUserDetailsService userDetailsService;
//
//    @Autowired
//    private PremiumCheckFilter premiumCheckFilter;
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/api/auth/**").permitAll()
   //                    .requestMatchers("/api/user/buyPremium/**").permitAll()
//                        .requestMatchers("/api/owner/buyPremiumByOwner/**").permitAll()
//                        .requestMatchers("/uploads/**").permitAll()
//                        .requestMatchers("/property-images/**").permitAll()
//                        .requestMatchers("/api/owner/save-facilities","/api/owner/get-facilities").permitAll()
//
//                        .requestMatchers("/api/area/**").permitAll()
//                        .requestMatchers("/api/owner/getAreasByCity/**", "/api/owner/getPincode").permitAll()
//                        .requestMatchers("/uploads/**", "/images/**", "/**/*.jpg", "/**/*.png", "/**/*.jpeg").permitAll()
//                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
//                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
//                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
//                        .requestMatchers(HttpMethod.GET, "/api/owner/getPropertyById/**")
//                        .permitAll()
//                        .requestMatchers(HttpMethod.GET, "/api/owner/property/image/**")
//                        .permitAll()
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/api/owner/**").hasAnyRole("PROPERTY_OWNER", "ADMIN")
//                        .requestMatchers("/api/auth/**","/api/auth/register").permitAll()
//                        .requestMatchers("/api/auth/**","/api/auth/register").permitAll()
//                        .requestMatchers("/chat/**","/socket.io/**").permitAll()
//                        .requestMatchers("/api/chat/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//
//                .authenticationProvider(authenticationProvider())
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterAfter(premiumCheckFilter, JwtFilter.class);
//                 return http.build();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
//
//    @Bean
//    public DaoAuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
//        provider.setPasswordEncoder(passwordEncoder());
//        return provider;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000","https://caryanambroker.vercel.app"));
//         //config.setAllowedOrigins(List.of("*"));
//        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        config.setAllowedHeaders(Arrays.asList("*"));
//        config.setAllowCredentials(true);
//        config.setExposedHeaders(Arrays.asList("Authorization"));
//        config.setMaxAge(3600L);
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }
//}

package com.caryanam.caryanam_broker.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PremiumCheckFilter premiumCheckFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/premium/callback", "/premium/verify/**", "/premium/payment-redirect").permitAll()
                        .requestMatchers("/premium/buy/**", "/premium/status/**").hasAnyRole("PROPERTY_OWNER", "ADMIN")
                        .requestMatchers("/admin/premium/**").hasRole("ADMIN")
//                        .requestMatchers("/api/user/buyPremium/**").permitAll()
                                .requestMatchers("/api/user/buyPremium/**")
                                .hasAnyRole("USER","ADMIN")
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/property-images/**").permitAll()
                        .requestMatchers("/api/owner/save-facilities","/api/owner/get-facilities").permitAll()

                        .requestMatchers("/api/area/**").permitAll()
                        .requestMatchers("/api/owner/getAreasByCity/**", "/api/owner/getPincode").permitAll()
                        .requestMatchers("/uploads/**", "/images/**", "/**/*.jpg", "/**/*.png", "/**/*.jpeg").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/user/likeProperty/**","/api/user/likedProperties","/api/user/likedPropertiesCount").hasAnyRole("USER")
                        .requestMatchers(HttpMethod.GET, "/api/owner/getPropertyById/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/owner/property/image/**")
                        .permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/owner/**").hasAnyRole("PROPERTY_OWNER", "ADMIN")
                        .requestMatchers("/api/auth/**","/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/**","/api/auth/register").permitAll()
                        .requestMatchers("/chat/**","/socket.io/**").permitAll()
                        .requestMatchers("/api/chat/**").permitAll()
                        // PhonePe Mobile SDK endpoints - authenticated users/owners only
                        .requestMatchers("/api/phonepe/mobile/create-order").hasAnyRole("USER", "PROPERTY_OWNER", "ADMIN")
                        .requestMatchers("/api/phonepe/mobile/verify/**").hasAnyRole("USER", "PROPERTY_OWNER", "ADMIN")
                        .anyRequest().authenticated()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(premiumCheckFilter, JwtFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOrigins(List.of(
//                "http://localhost:5173",
//                "http://localhost:3000",
//                "http://localhost:63342",
//                "https://rentalchaavi.netlify.app",
//                "https://r1.rentalchaavi.com",
//                "https://r2.rentalchaavi.com",
//                "https://rentalchaavi.com"
//        ));
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:63342",
                "https://rentalchaavi.netlify.app",
                "https://r1.rentalchaavi.com",
                "https://r2.rentalchaavi.com",
                "https://rentalchaavi.com",
                "https://www.rentalchaavi.com"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(Arrays.asList("Authorization"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
