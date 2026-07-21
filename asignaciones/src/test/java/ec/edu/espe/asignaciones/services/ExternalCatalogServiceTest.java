package ec.edu.espe.asignaciones.services;

import ec.edu.espe.asignaciones.dtos.UserRoleAssignmentResponse;
import ec.edu.espe.asignaciones.dtos.UsuarioClientResponse;
import ec.edu.espe.asignaciones.dtos.VehiculoClientResponse;
import ec.edu.espe.asignaciones.utils.RecursoNoEncontradoException;
import ec.edu.espe.asignaciones.utils.ReglaNegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ExternalCatalogService using mocked RestClient chain.
 *
 * <p>RestClient is a Spring 6 interface, so Mockito can mock it directly.
 * We intercept RestClient.Builder to return our mocked RestClient instance.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class ExternalCatalogServiceTest {

    private RestClient restClientMock;
    private ExternalCatalogService service;

    // Fluent chain mocks
    private RestClient.RequestHeadersUriSpec uriSpecMock;
    private RestClient.RequestHeadersSpec headersSpecMock;
    private RestClient.ResponseSpec responseSpecMock;

    @BeforeEach
    void setUp() {
        restClientMock = mock(RestClient.class);
        uriSpecMock = mock(RestClient.RequestHeadersUriSpec.class);
        headersSpecMock = mock(RestClient.RequestHeadersSpec.class);
        responseSpecMock = mock(RestClient.ResponseSpec.class);

        // Wire the builder so that restClientBuilder.requestFactory(...).build() returns our mock
        RestClient.Builder builderMock = mock(RestClient.Builder.class);
        when(builderMock.requestFactory(any())).thenReturn(builderMock);
        when(builderMock.build()).thenReturn(restClientMock);

        // Wire the fluent chain
        when(restClientMock.get()).thenReturn(uriSpecMock);
        when(uriSpecMock.uri(anyString(), any(Object[].class))).thenReturn(headersSpecMock);
        when(headersSpecMock.headers(any())).thenReturn(headersSpecMock);
        when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);

        service = new ExternalCatalogService(builderMock,
                "http://usuarios", "http://vehiculos");
    }

    // ========== validarUsuarioActivo ==========

    @Test
    void validarUsuarioActivo_usuarioActivo_retornaUsuario() {
        UsuarioClientResponse usuario = new UsuarioClientResponse();
        usuario.setId(UUID.randomUUID());
        usuario.setActive(true);

        when(responseSpecMock.body(UsuarioClientResponse.class)).thenReturn(usuario);

        UsuarioClientResponse resultado = service.validarUsuarioActivo(usuario.getId(), "Bearer tok");

        assertNotNull(resultado);
        assertTrue(resultado.isActive());
    }

    @Test
    void validarUsuarioActivo_notFound_lanzaRecursoNoEncontrado() {
        when(responseSpecMock.body(UsuarioClientResponse.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        UUID id = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.validarUsuarioActivo(id, null));
    }

    @Test
    void validarUsuarioActivo_bodyNull_lanzaRecursoNoEncontrado() {
        when(responseSpecMock.body(UsuarioClientResponse.class)).thenReturn(null);

        UUID id = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.validarUsuarioActivo(id, "Bearer tok"));
    }

    @Test
    void validarUsuarioActivo_usuarioInactivo_lanzaReglaNegocio() {
        UsuarioClientResponse usuario = new UsuarioClientResponse();
        usuario.setId(UUID.randomUUID());
        usuario.setActive(false);

        when(responseSpecMock.body(UsuarioClientResponse.class)).thenReturn(usuario);

        assertThrows(ReglaNegocioException.class,
                () -> service.validarUsuarioActivo(usuario.getId(), "Bearer tok"));
    }

    @Test
    void validarUsuarioActivo_authorizationNull_noAgregaHeader() {
        UsuarioClientResponse usuario = new UsuarioClientResponse();
        usuario.setId(UUID.randomUUID());
        usuario.setActive(true);

        when(responseSpecMock.body(UsuarioClientResponse.class)).thenReturn(usuario);

        // Should not throw, auth header simply omitted
        assertDoesNotThrow(() -> service.validarUsuarioActivo(usuario.getId(), null));
    }

    @Test
    void validarUsuarioActivo_authorizationBlank_noAgregaHeader() {
        UsuarioClientResponse usuario = new UsuarioClientResponse();
        usuario.setId(UUID.randomUUID());
        usuario.setActive(true);

        when(responseSpecMock.body(UsuarioClientResponse.class)).thenReturn(usuario);

        assertDoesNotThrow(() -> service.validarUsuarioActivo(usuario.getId(), "   "));
    }

    // ========== validarVehiculoActivo ==========

    @Test
    void validarVehiculoActivo_vehiculoActivo_retornaVehiculo() {
        VehiculoClientResponse vehiculo = new VehiculoClientResponse();
        vehiculo.setId(UUID.randomUUID());
        vehiculo.setActivo(true);

        when(responseSpecMock.body(VehiculoClientResponse.class)).thenReturn(vehiculo);

        VehiculoClientResponse resultado = service.validarVehiculoActivo(vehiculo.getId(), "Bearer tok");

        assertNotNull(resultado);
        assertTrue(resultado.isActivo());
    }

    @Test
    void validarVehiculoActivo_notFound_lanzaRecursoNoEncontrado() {
        when(responseSpecMock.body(VehiculoClientResponse.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        UUID id = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.validarVehiculoActivo(id, null));
    }

    @Test
    void validarVehiculoActivo_bodyNull_lanzaRecursoNoEncontrado() {
        when(responseSpecMock.body(VehiculoClientResponse.class)).thenReturn(null);

        UUID id = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.validarVehiculoActivo(id, "Bearer tok"));
    }

    @Test
    void validarVehiculoActivo_vehiculoInactivo_lanzaReglaNegocio() {
        VehiculoClientResponse vehiculo = new VehiculoClientResponse();
        vehiculo.setId(UUID.randomUUID());
        vehiculo.setActivo(false);

        when(responseSpecMock.body(VehiculoClientResponse.class)).thenReturn(vehiculo);

        assertThrows(ReglaNegocioException.class,
                () -> service.validarVehiculoActivo(vehiculo.getId(), "Bearer tok"));
    }

    // ========== validarRolAutorizadoParaAsignacion ==========

    @Test
    void validarRolAutorizado_conRolActivo_retornaRol() {
        UserRoleAssignmentResponse rol = new UserRoleAssignmentResponse();
        rol.setIdUser(UUID.randomUUID());
        rol.setRol("CONDUCTOR");
        rol.setActive(true);

        when(responseSpecMock.body(UserRoleAssignmentResponse[].class))
                .thenReturn(new UserRoleAssignmentResponse[]{rol});

        UserRoleAssignmentResponse resultado =
                service.validarRolAutorizadoParaAsignacion(rol.getIdUser(), "Bearer tok");

        assertEquals("CONDUCTOR", resultado.getRol());
    }

    @Test
    void validarRolAutorizado_notFound_lanzaRecursoNoEncontrado() {
        when(responseSpecMock.body(UserRoleAssignmentResponse[].class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        UUID id = UUID.randomUUID();
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.validarRolAutorizadoParaAsignacion(id, null));
    }

    @Test
    void validarRolAutorizado_bodyNull_lanzaReglaNegocio() {
        when(responseSpecMock.body(UserRoleAssignmentResponse[].class)).thenReturn(null);

        UUID id = UUID.randomUUID();
        assertThrows(ReglaNegocioException.class,
                () -> service.validarRolAutorizadoParaAsignacion(id, "Bearer tok"));
    }

    @Test
    void validarRolAutorizado_sinRolesActivos_lanzaReglaNegocio() {
        UserRoleAssignmentResponse rol = new UserRoleAssignmentResponse();
        rol.setIdUser(UUID.randomUUID());
        rol.setRol("CONDUCTOR");
        rol.setActive(false); // inactive

        when(responseSpecMock.body(UserRoleAssignmentResponse[].class))
                .thenReturn(new UserRoleAssignmentResponse[]{rol});

        assertThrows(ReglaNegocioException.class,
                () -> service.validarRolAutorizadoParaAsignacion(rol.getIdUser(), "Bearer tok"));
    }

    @Test
    void validarRolAutorizado_arrayVacio_lanzaReglaNegocio() {
        when(responseSpecMock.body(UserRoleAssignmentResponse[].class))
                .thenReturn(new UserRoleAssignmentResponse[0]);

        UUID id = UUID.randomUUID();
        assertThrows(ReglaNegocioException.class,
                () -> service.validarRolAutorizadoParaAsignacion(id, "Bearer tok"));
    }
}
