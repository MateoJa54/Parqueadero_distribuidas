package ec.edu.espe.tickets.services;

import ec.edu.espe.tickets.dtos.AsignacionActivaResponse;
import ec.edu.espe.tickets.dtos.EspacioClientResponse;
import ec.edu.espe.tickets.dtos.VehiculoClientResponse;
import ec.edu.espe.tickets.utils.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de CatalogoExternoService.
 * Se intercepta el RestClient.Builder para inyectar un RestClient mockeado.
 */
class CatalogoExternoServiceTest {

    private RestClient restClient;
    private CatalogoExternoService service;

    // Specs del RestClient encadenado
    private RestClient.RequestHeadersUriSpec<?> headersUriSpec;
    private RestClient.RequestHeadersSpec<?> headersSpec;
    private RestClient.ResponseSpec responseSpec;

    private RestClient.RequestBodyUriSpec bodyUriSpec;
    private RestClient.RequestBodySpec bodySpec;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        headersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);

        // Builder que devuelve nuestro RestClient mockeado
        RestClient.Builder builder = mock(RestClient.Builder.class);
        when(builder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        service = new CatalogoExternoService(
                builder,
                "http://vehiculos",
                "http://asignaciones",
                "http://zonas");
    }

    // Encadena get -> uri -> headers -> retrieve -> body
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubGetChain(Object returnBody) {
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) headersUriSpec);
        when(headersUriSpec.uri(anyString(), (Object[]) any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.headers(any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class))).thenReturn(returnBody);
    }

    // --------------- obtenerVehiculoPorPlaca ---------------

    @Test
    void obtenerVehiculoPorPlaca_happyPath_retornaVehiculo() {
        VehiculoClientResponse vehiculo = new VehiculoClientResponse();
        vehiculo.setId(UUID.randomUUID());
        vehiculo.setPlaca("ABC-1234");

        stubGetChain(vehiculo);

        VehiculoClientResponse result = service.obtenerVehiculoPorPlaca("ABC-1234");

        assertNotNull(result);
        assertEquals("ABC-1234", result.getPlaca());
    }

    @Test
    void obtenerVehiculoPorPlaca_bodyNull_lanzaRecursoNoEncontrado() {
        stubGetChain(null);

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerVehiculoPorPlaca("ABC-1234"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void obtenerVehiculoPorPlaca_404_lanzaRecursoNoEncontrado() {
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) headersUriSpec);
        when(headersUriSpec.uri(anyString(), (Object[]) any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.headers(any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerVehiculoPorPlaca("ZZZ-9999"));
    }

    // --------------- obtenerAsignacionActivaPorVehiculo ---------------

    @Test
    void obtenerAsignacionActivaPorVehiculo_happyPath_retornaAsignacion() {
        UUID vehicleId = UUID.randomUUID();
        AsignacionActivaResponse asignacion = new AsignacionActivaResponse();
        asignacion.setUserId(UUID.randomUUID());
        asignacion.setActive(true);

        stubGetChain(asignacion);

        AsignacionActivaResponse result = service.obtenerAsignacionActivaPorVehiculo(vehicleId);

        assertNotNull(result);
        assertTrue(result.isActive());
    }

    @Test
    void obtenerAsignacionActivaPorVehiculo_bodyNull_lanzaRecursoNoEncontrado() {
        stubGetChain(null);

        UUID vehicleId = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerAsignacionActivaPorVehiculo(vehicleId));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void obtenerAsignacionActivaPorVehiculo_404_lanzaRecursoNoEncontrado() {
        UUID vehicleId = UUID.randomUUID();
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) headersUriSpec);
        when(headersUriSpec.uri(anyString(), (Object[]) any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.headers(any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerAsignacionActivaPorVehiculo(vehicleId));
    }

    // --------------- obtenerEspacio ---------------

    @Test
    void obtenerEspacio_happyPath_retornaEspacio() {
        UUID idEspacio = UUID.randomUUID();
        EspacioClientResponse espacio = new EspacioClientResponse();
        espacio.setId(idEspacio);
        espacio.setCodigo("E-01");

        stubGetChain(espacio);

        EspacioClientResponse result = service.obtenerEspacio(idEspacio);

        assertNotNull(result);
        assertEquals("E-01", result.getCodigo());
    }

    @Test
    void obtenerEspacio_bodyNull_lanzaRecursoNoEncontrado() {
        stubGetChain(null);

        UUID idEspacio = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerEspacio(idEspacio));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void obtenerEspacio_404_lanzaRecursoNoEncontrado() {
        UUID idEspacio = UUID.randomUUID();
        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) headersUriSpec);
        when(headersUriSpec.uri(anyString(), (Object[]) any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.headers(any())).thenReturn((RestClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(Class.class)))
                .thenThrow(HttpClientErrorException.NotFound.class);

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.obtenerEspacio(idEspacio));
    }

    // --------------- cambiarEstadoEspacio ---------------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void cambiarEstadoEspacio_happyPath_ejecutaSinExcepcion() {
        UUID idEspacio = UUID.randomUUID();

        RestClient.ResponseSpec patchResponse = mock(RestClient.ResponseSpec.class);
        when(restClient.patch()).thenReturn((RestClient.RequestBodyUriSpec) bodyUriSpec);
        when(bodyUriSpec.uri(anyString(), any(), any())).thenReturn((RestClient.RequestBodySpec) bodySpec);
        when(bodySpec.headers(any())).thenReturn((RestClient.RequestBodySpec) bodySpec);
        when(bodySpec.retrieve()).thenReturn(patchResponse);
        when(patchResponse.toBodilessEntity()).thenReturn(null);

        assertDoesNotThrow(() -> service.cambiarEstadoEspacio(idEspacio, "DISPONIBLE"));
    }
}
