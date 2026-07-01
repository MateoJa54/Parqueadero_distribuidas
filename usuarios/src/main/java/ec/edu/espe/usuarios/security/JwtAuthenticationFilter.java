package ec.edu.espe.usuarios.security;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtro que lee el header {@code Authorization: Bearer <token>}, valida el JWT
 * y coloca la autenticacion en el contexto de seguridad.
 *
 * El principal es el id del usuario y las authorities son {@code ROLE_<rol>}
 * por cada rol activo. Si no hay token o es invalido, NO se autentica: la
 * peticion sigue como anonima (rol invitado) y las reglas de autorizacion
 * decidiran si el endpoint es publico o responde 401.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                // Solo un ACCESS token autoriza peticiones; un refresh token aqui es invalido.
                Claims claims = jwtService.validarTipo(token, JwtService.TYPE_ACCESS);
                List<String> roles = claims.get("roles", List.class);
                List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                        .toList();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                // Token invalido/expirado: se ignora y la peticion sigue anonima.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
