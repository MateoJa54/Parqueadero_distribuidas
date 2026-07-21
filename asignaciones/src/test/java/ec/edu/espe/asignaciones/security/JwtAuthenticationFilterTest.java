package ec.edu.espe.asignaciones.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtServiceMock;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtServiceMock = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtServiceMock);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- helpers ----

    private MockHttpServletRequest requestWith(String authHeader) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (authHeader != null) {
            req.addHeader("Authorization", authHeader);
        }
        return req;
    }

    private Claims fakeClaims(String subject, List<String> roles) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("roles", List.class)).thenReturn(roles);
        return claims;
    }

    // ---- no header → chain proceeds, no auth set ----

    @Test
    void sinAuthorizationHeader_chainProcede_sinAutenticacion() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith(null);
        HttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtServiceMock);
    }

    // ---- non-Bearer header → chain proceeds ----

    @Test
    void headerNoBearer_chainProcede_sinAutenticacion() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith("Basic dXNlcjpwYXNz");
        HttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtServiceMock);
    }

    // ---- valid Bearer → auth set in context ----

    @Test
    void bearerValido_autenticacionColocadaEnContexto() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith("Bearer valid.token.here");
        HttpServletResponse res = new MockHttpServletResponse();

        Claims claims = fakeClaims("user-uuid-123", List.of("CONDUCTOR"));
        when(jwtServiceMock.validarAccessToken("valid.token.here")).thenReturn(claims);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user-uuid-123", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    // ---- valid Bearer with null roles ----

    @Test
    void bearerValido_rolesNull_autenticacionSinAuthorities() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith("Bearer valid.token.noroles");
        HttpServletResponse res = new MockHttpServletResponse();

        Claims claims = fakeClaims("user-uuid-456", null);
        when(jwtServiceMock.validarAccessToken("valid.token.noroles")).thenReturn(claims);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
    }

    // ---- JwtException → context cleared, chain proceeds ----

    @Test
    void tokenInvalido_jwtException_contextoClearY_chainProcede() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith("Bearer bad.token");
        HttpServletResponse res = new MockHttpServletResponse();

        when(jwtServiceMock.validarAccessToken("bad.token"))
                .thenThrow(new JwtException("token invalido"));

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ---- IllegalArgumentException → context cleared, chain proceeds ----

    @Test
    void tokenInvalido_illegalArgumentException_contextoClearY_chainProcede() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith("Bearer illegal.token");
        HttpServletResponse res = new MockHttpServletResponse();

        when(jwtServiceMock.validarAccessToken("illegal.token"))
                .thenThrow(new IllegalArgumentException("argumento invalido"));

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ---- context already has auth → skips validation ----

    @Test
    void contextoYaTieneAuth_saltaValidacion() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith("Bearer some.token");
        HttpServletResponse res = new MockHttpServletResponse();

        // Pre-populate context with existing auth
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing-user", null, List.of()));

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        verifyNoInteractions(jwtServiceMock);
        assertEquals("existing-user",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }

    // ---- Bearer with multiple roles ----

    @Test
    void bearerValido_variosRoles_authoritiesConPrefixROLE() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest req = requestWith("Bearer multi.role.token");
        HttpServletResponse res = new MockHttpServletResponse();

        Claims claims = fakeClaims("user-uuid-multi", List.of("CONDUCTOR", "ADMIN"));
        when(jwtServiceMock.validarAccessToken("multi.role.token")).thenReturn(claims);

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(2, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CONDUCTOR".equals(a.getAuthority())));
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
    }
}
