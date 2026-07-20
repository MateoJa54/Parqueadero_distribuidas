package ec.edu.espe.zonas.config;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.espe.zonas.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Seguridad del microservicio zonas.
 *
 * <ul>
 *   <li>API stateless (sin sesion), CSRF deshabilitado.</li>
 *   <li>Toda peticion exige un access token valido.</li>
 *   <li>La autorizacion FINA por rol (ADMIN/ROOT para administrar, RECAUDADOR
 *       ademas para cambiar el estado de un espacio) se declara con
 *       {@code @PreAuthorize} en cada controlador.</li>
 *   <li>401/403 se devuelven como JSON coherente con el GlobalExceptionHandler.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(cto -> {})
                        .frameOptions(fo -> fo.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(rp -> rp
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)))
                .authorizeHttpRequests(auth -> auth
                        // Canal SSE del dashboard: solo lectura y sin datos sensibles.
                        // EventSource no puede enviar el token, por eso es publico.
                        .requestMatchers("/api/v1/sse/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> escribirError(res,
                                HttpStatus.UNAUTHORIZED, "Token ausente o invalido: inicie sesion"))
                        .accessDeniedHandler((req, res, e) -> escribirError(res,
                                HttpStatus.FORBIDDEN, "No tiene permisos para esta operacion")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
