package com.resale.loveresaleappointment.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    // Secrets per issuer
    private static final Map<String, String> ISSUER_SECRETS = Map.of(
            "vso-auth", "MySuperSecureLongSecretKeyThatIsAtLeast32Bytes!",
            "user-ms", "AnotherVerySecureSecretKeyThatIs32PlusChars!"
    );


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CookieBearerTokenResolver cookieBearerTokenResolver) throws Exception {
        Map<String, AuthenticationManager> authManagers = new HashMap<>();

        ISSUER_SECRETS.forEach((issuer, secret) -> {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "HmacSHA256");

            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();
            jwtDecoder.setJwtValidator(JwtValidators.createDefault());

            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(jwtDecoder);
            provider.setJwtAuthenticationConverter(jwtAuthenticationConverter());

            authManagers.put(issuer, new ProviderManager(provider));
        });

        JwtIssuerAuthenticationManagerResolver resolver =
                new JwtIssuerAuthenticationManagerResolver(authManagers::get);

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
//                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                .requestMatchers("/internal/**").permitAll()
                                .requestMatchers("/queue/sendToQueue").authenticated()
                                .requestMatchers("/queue/**").permitAll()
                                .requestMatchers("/customer/{appointmentId}").permitAll()
                                .requestMatchers("/customer/otp/verify").permitAll()
                                .requestMatchers("/customer/customerOnCall/{customerId}").permitAll()
                                .requestMatchers("/customer/**").hasRole("CUSTOMER")
                                .requestMatchers("/user/**").authenticated()
                                .requestMatchers("/teamLead/**").authenticated()
                                .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .authenticationManagerResolver(resolver)
                );

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); // claim from Customer MS / User MS
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        System.out.println("Roles : "+grantedAuthoritiesConverter);
        return converter;
    }
}

