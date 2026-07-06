package ec.edu.espe.tickets.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/** Asignacion activa de un vehiculo, devuelta por el microservicio asignaciones. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsignacionActivaResponse {

    private UUID userId;
    private UUID vehicleId;
    private boolean active;
    private String status;
    private boolean entryAuthorized;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;

    /** Tipo de asignacion (PROPIETARIO, AUTORIZADO, TEMPORAL). */
    private String assignmentType;

    /** Rol del propietario; se usa como categoria para la tarifa diferenciada por rol. */
    @JsonProperty("authorizationRoleName")
    private String rolAutorizacion;
}
