package ec.edu.espe.tickets.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
}
