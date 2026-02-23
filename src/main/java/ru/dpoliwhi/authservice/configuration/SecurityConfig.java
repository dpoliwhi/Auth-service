package ru.dpoliwhi.authservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/manager.html").hasRole("MANAGER")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .oauth2Login(Customizer.withDefaults());
        return http.build();
    }


    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtAuthenticationConverter.setPrincipalClaimName("preferred_username");
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
            List<GrantedAuthority> combined = new ArrayList<>(authorities);

            List<GrantedAuthority> mappedRoles = getCustomRoles(jwt.getClaimAsMap("resource_access"));
            combined.addAll(mappedRoles);

            return combined;
        });
        return jwtAuthenticationConverter;
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oAuth2UserService() {
        OidcUserService oidcUserService = new OidcUserService();
        return userRequest -> {
            OidcUser oidcUser = oidcUserService.loadUser(userRequest);

            List<GrantedAuthority> combined = new ArrayList<>(oidcUser.getAuthorities());

            List<GrantedAuthority> mappedRoles = getCustomRoles(oidcUser.getClaimAsMap("resource_access"));
            combined.addAll(mappedRoles);

            return new DefaultOidcUser(combined, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    private List<GrantedAuthority> getCustomRoles(Map<String, Object> resourceAccess) {
        if (resourceAccess == null) {
            return List.of();
        }
        Object backendObj = resourceAccess.get("backend");
        if (!(backendObj instanceof Map)) {
            return List.of();
        }

        Map<String, Object> backend = (Map<String, Object>) backendObj;
        Object rolesObj = backend.get("roles");
        if (!(rolesObj instanceof List)) {
            return List.of();
        }

        List<String> roles = (List<String>) rolesObj;
        return roles.stream()
                .filter(role -> role != null && role.startsWith("ROLE_"))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
    }
}