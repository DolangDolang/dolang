package live.dolang.api.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import live.dolang.api.common.response.BaseResponse;
import live.dolang.api.common.response.BaseResponseStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(request ->
                        request
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                                .requestMatchers("/api/feed/today/**").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer
                                .jwt(Customizer.withDefaults())
                                .authenticationEntryPoint((request, response, authException) -> {
                                            BaseResponse<?> errorResponse = BaseResponse.status(BaseResponseStatus.INVALID_JWT);
                                            response.setContentType("application/json");
                                            response.setCharacterEncoding("UTF-8");
                                            response.setStatus(HttpServletResponse.SC_OK);
                                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                                        }
                                )
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:5173", "https://dolang.live"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowedMethods(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig); // Apply to all paths
        return source;
    }

}
