package ec.edu.espe.asignaciones.services;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import ec.edu.espe.asignaciones.dtos.UserRoleAssignmentResponse;
import ec.edu.espe.asignaciones.dtos.UsuarioClientResponse;
import ec.edu.espe.asignaciones.dtos.VehiculoClientResponse;
import ec.edu.espe.asignaciones.utils.RecursoNoEncontradoException;
import ec.edu.espe.asignaciones.utils.ReglaNegocioException;

@Service
public class ExternalCatalogService {

    private final RestClient restClient;
    private final String usuariosUrl;
    private final String vehiculosUrl;

    public ExternalCatalogService(
            RestClient.Builder restClientBuilder,
            @Value("${services.usuarios-url}") String usuariosUrl,
            @Value("${services.vehiculos-url}") String vehiculosUrl) {
        this.restClient = restClientBuilder.build();
        this.usuariosUrl = usuariosUrl;
        this.vehiculosUrl = vehiculosUrl;
    }

    public UsuarioClientResponse validarUsuarioActivo(UUID userId, String authorization) {
        UsuarioClientResponse usuario;
        try {
            usuario = restClient.get()
                    .uri(usuariosUrl + "/api/v1/usuarios/{idUsuario}", userId)
                    .headers(headers -> agregarAuthorization(headers, authorization))
                    .retrieve()
                    .body(UsuarioClientResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Usuario no encontrado: " + userId);
        }

        if (usuario == null) {
            throw new RecursoNoEncontradoException("Usuario no encontrado: " + userId);
        }
        if (!usuario.isActive()) {
            throw new ReglaNegocioException("El usuario no esta activo: " + userId);
        }
        return usuario;
    }

    public VehiculoClientResponse validarVehiculoActivo(UUID vehicleId) {
        VehiculoClientResponse vehiculo;
        try {
            vehiculo = restClient.get()
                    .uri(vehiculosUrl + "/api/vehiculos/{id}", vehicleId)
                    .retrieve()
                    .body(VehiculoClientResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Vehiculo no encontrado: " + vehicleId);
        }

        if (vehiculo == null) {
            throw new RecursoNoEncontradoException("Vehiculo no encontrado: " + vehicleId);
        }
        if (!vehiculo.isActivo()) {
            throw new ReglaNegocioException("El vehiculo no esta activo: " + vehicleId);
        }
        return vehiculo;
    }

    public UserRoleAssignmentResponse validarRolAutorizadoParaAsignacion(UUID userId, String authorization) {
        UserRoleAssignmentResponse[] roles;
        try {
            roles = restClient.get()
                    .uri(usuariosUrl + "/api/v1/asignaciones/usuario/{idUsuario}", userId)
                    .headers(headers -> agregarAuthorization(headers, authorization))
                    .retrieve()
                    .body(UserRoleAssignmentResponse[].class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("Usuario no encontrado: " + userId);
        }

        return Arrays.stream(roles != null ? roles : new UserRoleAssignmentResponse[0])
                .filter(UserRoleAssignmentResponse::isActive)
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "El usuario no tiene un rol activo para autorizar la asignacion"));
    }

    /** Reenvia el token del llamante original: usuarios exige ADMIN/ROOT en estos endpoints. */
    private void agregarAuthorization(HttpHeaders headers, String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            headers.set("Authorization", authorization);
        }
    }
}
