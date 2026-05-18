package com.example.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Spring Security 6 Config làm OAuth2 Resource Server để xác thực JWT Access Token từ Keycloak.
 * Bảo vệ toàn bộ tài nguyên API của dự án với các Best Practice về bảo mật.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Cấu hình CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2. Vô hiệu hóa CSRF vì hệ thống sử dụng stateless JWT token
            .csrf(AbstractHttpConfigurer::disable)
            // 3. Quản lý session ở chế độ STATELESS
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 4. Phân quyền truy cập các Endpoint
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/leave-requests/submit").permitAll() // Đơn gửi công khai
                .requestMatchers("/api/v1/public/**").permitAll()
                .anyRequest().authenticated() // Tất cả các API còn lại đều phải đăng nhập bằng Token
            )
            // 5. Cấu hình JWT Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * Converter bóc tách Roles từ Keycloak JWT claims và chuyển đổi thành GrantedAuthority của Spring Security.
     * Hỗ trợ tìm kiếm cả trong realm_access.roles và resource_access.{client_id}.roles
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Trình chuyển đổi mặc định để lấy các SCOPE_ từ JWT
        JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        converter.setJwtGrantedAuthoritiesConverter(new Converter<Jwt, Collection<GrantedAuthority>>() {
            @Override
            public Collection<GrantedAuthority> convert(Jwt jwt) {
                // 1. Lấy các quyền hạn dạng SCOPE_ mặc định
                Collection<GrantedAuthority> defaultAuthorities = defaultAuthoritiesConverter.convert(jwt);
                if (defaultAuthorities == null) {
                    defaultAuthorities = Collections.emptyList();
                }

                // 2. Trích xuất Realm-level Roles từ trường 'realm_access.roles'
                Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

                // 3. Trích xuất Client-level Roles từ trường 'resource_access'
                Collection<GrantedAuthority> clientRoles = extractClientRoles(jwt);

                // Gộp tất cả các Authority lại với nhau
                return Stream.concat(
                        Stream.concat(defaultAuthorities.stream(), realmRoles.stream()),
                        clientRoles.stream()
                ).collect(Collectors.toSet());
            }
        });
        
        return converter;
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) {
            return Collections.emptyList();
        }
        
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return Collections.emptyList();
        }

        return resourceAccess.entrySet().stream()
                .flatMap(entry -> {
                    Map<String, Object> clientAccess = (Map<String, Object>) entry.getValue();
                    if (clientAccess == null || !clientAccess.containsKey("roles")) {
                        return Stream.empty();
                    }
                    List<String> roles = (List<String>) clientAccess.get("roles");
                    String clientName = entry.getKey().toUpperCase();
                    return roles.stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + clientName + "_" + role.toUpperCase()));
                })
                .collect(Collectors.toList());
    }

    /**
     * Cấu hình CORS động cho phép Next.js Frontend gọi API an toàn.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); // Port chạy Next.js
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
