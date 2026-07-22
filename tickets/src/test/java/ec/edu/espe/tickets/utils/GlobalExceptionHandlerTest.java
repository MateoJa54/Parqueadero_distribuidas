package ec.edu.espe.tickets.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidation_retorna400ConErrores() {
        FieldError fieldError = new FieldError("obj", "placa", "La placa es obligatoria");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("errores"));
    }

    @Test
    void handleNoEncontrado_retorna404() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("ticket no existe");
        ResponseEntity<Map<String, Object>> response = handler.handleNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("ticket no existe", response.getBody().get("mensaje"));
    }

    @Test
    void handleReglaNegocio_retorna409() {
        ReglaNegocioException ex = new ReglaNegocioException("vehiculo inactivo");
        ResponseEntity<Map<String, Object>> response = handler.handleReglaNegocio(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("vehiculo inactivo", response.getBody().get("mensaje"));
    }

    @Test
    void handleAccessDenied_retorna403() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
    }

    @Test
    void handleIntegridad_retorna409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation");
        ResponseEntity<Map<String, Object>> response = handler.handleIntegridad(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
    }

    @Test
    void handleServicioCaido_retorna503() {
        ResourceAccessException ex = new ResourceAccessException("connection refused");
        ResponseEntity<Map<String, Object>> response = handler.handleServicioCaido(ex);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getBody().get("status"));
    }

    @Test
    void handleServicioExterno_retorna502() {
        ServicioExternoException ex = new ServicioExternoException("servicio caido");
        ResponseEntity<Map<String, Object>> response = handler.handleServicioExterno(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().get("status"));
        assertEquals("servicio caido", response.getBody().get("mensaje"));
    }

    @Test
    void handleRemote_conStatusConocido_retornaEseStatus() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "not found", null, null, null);
        ResponseEntity<Map<String, Object>> response = handler.handleRemote(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void handleRemote_conStatusNoEstandar_retornaBAD_GATEWAY() {
        // HttpStatus.resolve devuelve null para codigos no estandar => fallback BAD_GATEWAY
        HttpClientErrorException ex = mock(HttpClientErrorException.class);
        when(ex.getStatusCode()).thenReturn(HttpStatusCode.valueOf(499));
        ResponseEntity<Map<String, Object>> response = handler.handleRemote(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(502, response.getBody().get("status"));
    }

    @Test
    void handleRuntime_retorna500() {
        RuntimeException ex = new RuntimeException("error inesperado");
        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("error inesperado", response.getBody().get("mensaje"));
    }

    @Test
    void handleRuntime_mensajeNulo_retornaMensajePorDefecto() {
        RuntimeException ex = new RuntimeException((String) null);
        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error inesperado", response.getBody().get("mensaje"));
    }

    @Test
    void handleValidation_sinErroresDeCampo_retornaCuerpoVacio() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> errores = (Map<String, String>) response.getBody().get("errores");
        assertTrue(errores.isEmpty());
    }
}
