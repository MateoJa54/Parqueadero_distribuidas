package ec.edu.espe.tickets.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import ec.edu.espe.tickets.dtos.AsignacionActivaResponse;
import ec.edu.espe.tickets.dtos.EspacioClientResponse;
import ec.edu.espe.tickets.dtos.VehiculoClientResponse;
import ec.edu.espe.tickets.utils.RecursoNoEncontradoException;

/**
 * Cliente de los microservicios que tickets orquesta: vehiculos, asignaciones y
 * zonas/espacios. Encapsula las llamadas HTTP y traduce el 404 remoto a
 * {@link RecursoNoEncontradoException}; el resto de errores se propagan al
 * manejador global (5xx remoto, servicio caido, etc.).
 */
@Service
public class CatalogoExternoService {

    private final RestClient restClient;
    private final String vehiculosUrl;
    private final String asignacionesUrl;
    private final String zonasUrl;

    public CatalogoExternoService(
            RestClient.Builder restClientBuilder,
            @Value("${services.vehiculos-url}") String vehiculosUrl,
            @Value("${services.asignaciones-url}") String asignacionesUrl,
            @Value("${services.zonas-url}") String zonasUrl) {
        this.restClient = restClientBuilder.build();
        this.vehiculosUrl = vehiculosUrl;
        this.asignacionesUrl = asignacionesUrl;
        this.zonasUrl = zonasUrl;
    }

    /** Busca el vehiculo por placa en el microservicio vehiculos. */
    public VehiculoClientResponse obtenerVehiculoPorPlaca(String placa) {
        try {
            VehiculoClientResponse vehiculo = restClient.get()
                    .uri(vehiculosUrl + "/api/vehiculos/placa/{placa}", placa)
                    .retrieve()
                    .body(VehiculoClientResponse.class);
            if (vehiculo == null) {
                throw new RecursoNoEncontradoException("No existe un vehiculo con placa: " + placa);
            }
            return vehiculo;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe un vehiculo con placa: " + placa);
        }
    }

    /** Obtiene la (unica) asignacion activa del vehiculo en el microservicio asignaciones. */
    public AsignacionActivaResponse obtenerAsignacionActivaPorVehiculo(UUID vehicleId) {
        try {
            AsignacionActivaResponse asignacion = restClient.get()
                    .uri(asignacionesUrl + "/api/v1/asignaciones-vehiculos/vehiculo/{vehicleId}", vehicleId)
                    .retrieve()
                    .body(AsignacionActivaResponse.class);
            if (asignacion == null) {
                throw new RecursoNoEncontradoException(
                        "El vehiculo no tiene una asignacion activa: " + vehicleId);
            }
            return asignacion;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException(
                    "El vehiculo no tiene una asignacion activa: " + vehicleId);
        }
    }

    /** Obtiene el espacio por id en el microservicio zonas. */
    public EspacioClientResponse obtenerEspacio(UUID idEspacio) {
        try {
            EspacioClientResponse espacio = restClient.get()
                    .uri(zonasUrl + "/api/v1/espacios/{idEspacio}", idEspacio)
                    .retrieve()
                    .body(EspacioClientResponse.class);
            if (espacio == null) {
                throw new RecursoNoEncontradoException("No existe el espacio: " + idEspacio);
            }
            return espacio;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new RecursoNoEncontradoException("No existe el espacio: " + idEspacio);
        }
    }

    /** Cambia el estado de un espacio (OCUPADO al ingresar, DISPONIBLE al salir/anular). */
    public void cambiarEstadoEspacio(UUID idEspacio, String estado) {
        restClient.patch()
                .uri(zonasUrl + "/api/v1/espacios/{idEspacio}/estado?estado={estado}", idEspacio, estado)
                .retrieve()
                .toBodilessEntity();
    }
}
