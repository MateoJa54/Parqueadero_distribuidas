package ec.edu.espe.usuarios.config;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.espe.usuarios.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Configuracion de seguridad del microservicio usuarios (servidor de AUTH).
 *
 * Estrategia:
 *  - API stateless (sin sesion), CSRF deshabilitado (no hay formularios).
 *  - {@code /api/v1/auth/login} y {@code /api/v1/auth/register} son publicos
 *    (un invitado puede autenticarse o registrarse).
 *  - El resto exige token valido; la autorizacion FINA por rol se declara con
 *    {@code @PreAuthorize} en cada metodo de controlador (facil de replicar).
 *  - 401/403 se devuelven como JSON coherente con el GlobalExceptionHandler.
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh")
                        .permitAll()
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
