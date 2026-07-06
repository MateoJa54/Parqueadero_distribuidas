package ec.edu.espe.tickets.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ticket de parqueadero: documento que representa la estadia de un vehiculo en
 * un espacio, desde el ingreso hasta el pago o la anulacion.
 *
 * <p>Guarda "snapshots" (placa, tipos, codigo de espacio) porque el ticket es un
 * registro historico: aunque el vehiculo o el espacio cambien despues, el ticket
 * debe seguir siendo valido y auditable.
 */
@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_codigo", columnList = "codigo", unique = true),
        @Index(name = "idx_ticket_vehiculo_estado", columnList = "id_vehiculo, estado_ticket"),
        @Index(name = "idx_ticket_espacio_estado", columnList = "id_espacio, estado_ticket")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Codigo unico legible, formato "TKT-000001". */
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    // --- Ubicacion (espacio donde se estaciona) ---
    @Column(name = "id_espacio", nullable = false)
    private UUID idEspacio;

    @Column(name = "codigo_espacio", length = 20)
    private String codigoEspacio;

    @Column(name = "tipo_espacio", nullable = false, length = 20)
    private String tipoEspacio;

    // --- Identidad del ingreso (proviene de asignaciones) ---
    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "id_vehiculo", nullable = false)
    private UUID idVehiculo;

    @Column(nullable = false, length = 20)
    private String placa;

    @Column(name = "tipo_vehiculo", nullable = false, length = 20)
    private String tipoVehiculo;

    /**
     * Categoria usada para la tarifa diferenciada por rol (snapshot del rol del
     * propietario al momento del ingreso, ej. CLIENTE, INVITADO). Se guarda para
     * que el cobro sea reproducible aunque el rol cambie despues.
     */
    @Column(name = "categoria_tarifa", length = 40)
    private String categoriaTarifa;

    // --- Tiempos ---
    @Column(name = "fecha_hora_ingreso", nullable = false)
    private OffsetDateTime fechaHoraIngreso;

    @Column(name = "fecha_hora_salida")
    private OffsetDateTime fechaHoraSalida;

    // --- Estado y cobro ---
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_ticket", nullable = false, length = 12)
    private EstadoTicket estadoTicket;

    /** Empleado (RECAUDADOR) que opero el ticket; se toma del JWT de sesion. */
    @Column(name = "id_empleado", nullable = false)
    private UUID idEmpleado;

    @Column(name = "valor_recaudado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorRecaudado;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
