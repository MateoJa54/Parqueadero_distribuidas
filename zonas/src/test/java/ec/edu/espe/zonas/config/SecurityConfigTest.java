package ec.edu.espe.zonas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.espe.zonas.security.JwtAuthenticationFilter;

/** Verifica la construccion de la cadena de seguridad y el serializador de errores. */
class SecurityConfigTest {

    @Test
    void filterChainConfiguraYConstruyeCadena() throws Exception {
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        ObjectMapper mapper = new ObjectMapper();
        SecurityConfig config = new SecurityConfig(jwtFilter, mapper);

        HttpSecurity http = mock(HttpSecurity.class, org.mockito.Answers.RETURNS_SELF);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = config.filterChain(http);

        assertSame(chain, result);
        verify(http).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void escribirErrorSerializaJsonConStatusYMensaje() throws Exception {
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        ObjectMapper mapper = new ObjectMapper();
        SecurityConfig config = new SecurityConfig(jwtFilter, mapper);

        MockHttpServletResponse response = new MockHttpServletResponse();
        ReflectionTestUtils.invokeMethod(config, "escribirError",
                response, HttpStatus.FORBIDDEN, "No tiene permisos");

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));

        Map<String, Object> body = mapper.readValue(response.getContentAsString(), Map.class);
        assertEquals(403, body.get("status"));
        assertEquals("No tiene permisos", body.get("mensaje"));
        assertEquals(HttpStatus.FORBIDDEN.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }
}
