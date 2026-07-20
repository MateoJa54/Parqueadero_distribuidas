package ec.edu.espe.usuarios.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void jsonMalformadoResponde400SinExponerDetallesDelParser() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        ResponseEntity<Map<String, Object>> response = handler.handleMalformedJson(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El cuerpo JSON es invalido o no se puede leer", response.getBody().get("mensaje"));
    }

    @Test
    void uuidInvalidoResponde400() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("idUsuario");

        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Parametro invalido: idUsuario", response.getBody().get("mensaje"));
    }

    @Test
    void errorInesperadoNoFiltraElMensajeInterno() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntime(new RuntimeException("password=secreto; SQLSTATE=XX000"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error interno del servidor", response.getBody().get("mensaje"));
    }
}
