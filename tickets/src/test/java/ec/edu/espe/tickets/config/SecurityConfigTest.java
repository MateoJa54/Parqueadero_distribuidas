package ec.edu.espe.tickets.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ec.edu.espe.tickets.security.JwtAuthenticationFilter;
import ec.edu.espe.tickets.services.TicketService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Arranca la cadena de seguridad REAL (slice @WebMvcTest importando
 * {@link SecurityConfig}) para ejecutar cada lambda de configuracion y
 * el serializador de errores 401/403.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private TicketService ticketService;

    @Test
    void contextoArranca_cadenaDeSeguridadConstruida() {
        // El solo arranque del slice @WebMvcTest construye el SecurityFilterChain
        // ejecutando cada lambda de configuracion de filterChain().
        assertNotNull(mockMvc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void escribirError_401_serializaJson() throws Exception {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthenticationFilter.class), new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ReflectionTestUtils.invokeMethod(config, "escribirError",
                response, HttpStatus.UNAUTHORIZED, "Token ausente o invalido: inicie sesion");

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        Map<String, Object> body = new ObjectMapper().readValue(response.getContentAsString(), Map.class);
        assertEquals(401, body.get("status"));
        assertEquals("Token ausente o invalido: inicie sesion", body.get("mensaje"));
        assertEquals(HttpStatus.UNAUTHORIZED.getReasonPhrase(), body.get("error"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void escribirError_403_serializaJson() throws Exception {
        SecurityConfig config = new SecurityConfig(mock(JwtAuthenticationFilter.class), new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        ReflectionTestUtils.invokeMethod(config, "escribirError",
                response, HttpStatus.FORBIDDEN, "No tiene permisos para operar tickets");

        assertEquals(403, response.getStatus());
        Map<String, Object> body = new ObjectMapper().readValue(response.getContentAsString(), Map.class);
        assertEquals(403, body.get("status"));
        assertEquals("No tiene permisos para operar tickets", body.get("mensaje"));
        assertEquals(HttpStatus.FORBIDDEN.getReasonPhrase(), body.get("error"));
    }
}
