package ec.edu.espe.asignaciones.config;

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

import ec.edu.espe.asignaciones.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Seguridad del microservicio asignaciones.
 *
 * <ul>
 *   <li>API stateless (sin sesion), CSRF deshabilitado.</li>
 *   <li>Toda peticion exige un access token valido.</li>
 *   <li>La autorizacion FINA por rol se declara con {@code @PreAuthorize} en
 *       cada controlador (ADMIN/ROOT administran; RECAUDADOR consulta la
 *       asignacion activa de un vehiculo; un propietario consulta su propia
 *       flota).</li>
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
                .authorizeHttpRequests(auth -> auth
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
