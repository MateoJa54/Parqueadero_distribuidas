package ec.edu.espe.tickets.dtos;

import ec.edu.espe.tickets.entities.EstadoTicket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtosTest {

    @Test
    void registrarIngresoRequest_gettersSetters() {
        RegistrarIngresoRequest req = new RegistrarIngresoRequest();
        UUID idEspacio = UUID.randomUUID();
        req.setPlaca("ABC-1234");
        req.setIdEspacio(idEspacio);

        assertEquals("ABC-1234", req.getPlaca());
        assertEquals(idEspacio, req.getIdEspacio());
    }

    @Test
    void anularTicketRequest_gettersSetters() {
        AnularTicketRequest req = new AnularTicketRequest();
        req.setMotivo("placa equivocada");
        assertEquals("placa equivocada", req.getMotivo());
    }

    @Test
    void ticketResponse_builder_conTodosLosCampos() {
        UUID id = UUID.randomUUID();
        TicketResponse resp = TicketResponse.builder()
                .id(id)
                .codigo("TKT-000001")
                .estadoTicket(EstadoTicket.ACTIVO)
                .valorRecaudado(BigDecimal.ZERO)
                .build();

        assertEquals(id, resp.getId());
        assertEquals("TKT-000001", resp.getCodigo());
        assertEquals(EstadoTicket.ACTIVO, resp.getEstadoTicket());
        assertEquals(BigDecimal.ZERO, resp.getValorRecaudado());
    }

    @Test
    void vehiculoClientResponse_gettersSetters() {
        VehiculoClientResponse v = new VehiculoClientResponse();
        UUID id = UUID.randomUUID();
        v.setId(id);
        v.setPlaca("XYZ-9999");
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        v.setColor("Rojo");
        v.setAnio(2020);
        v.setClasificacion("A");
        v.setTipo("Auto");
        v.setActivo(true);

        assertEquals(id, v.getId());
        assertEquals("XYZ-9999", v.getPlaca());
        assertEquals("Toyota", v.getMarca());
        assertEquals("Corolla", v.getModelo());
        assertEquals("Rojo", v.getColor());
        assertEquals(2020, v.getAnio());
        assertEquals("A", v.getClasificacion());
        assertEquals("Auto", v.getTipo());
        assertTrue(v.isActivo());
    }

    @Test
    void espacioClientResponse_gettersSetters() {
        EspacioClientResponse e = new EspacioClientResponse();
        UUID id = UUID.randomUUID();
        UUID idZona = UUID.randomUUID();
        e.setId(id);
        e.setCodigo("E-01");
        e.setDescripcion("Espacio 1");
        e.setTipo("AUTO");
        e.setActivo(true);
        e.setIdZona(idZona);
        e.setNombreZona("Zona A");
        e.setEstado("DISPONIBLE");

        assertEquals(id, e.getId());
        assertEquals("E-01", e.getCodigo());
        assertEquals("Espacio 1", e.getDescripcion());
        assertEquals("AUTO", e.getTipo());
        assertTrue(e.isActivo());
        assertEquals(idZona, e.getIdZona());
        assertEquals("Zona A", e.getNombreZona());
        assertEquals("DISPONIBLE", e.getEstado());
    }

    @Test
    void asignacionActivaResponse_gettersSetters() {
        AsignacionActivaResponse a = new AsignacionActivaResponse();
        UUID userId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime validFrom = OffsetDateTime.now().minusDays(1);
        OffsetDateTime validUntil = OffsetDateTime.now().plusDays(1);

        a.setUserId(userId);
        a.setVehicleId(vehicleId);
        a.setActive(true);
        a.setStatus("ACTIVA");
        a.setEntryAuthorized(true);
        a.setValidFrom(validFrom);
        a.setValidUntil(validUntil);
        a.setAssignmentType("PROPIETARIO");
        a.setRolAutorizacion("CLIENTE");

        assertEquals(userId, a.getUserId());
        assertEquals(vehicleId, a.getVehicleId());
        assertTrue(a.isActive());
        assertEquals("ACTIVA", a.getStatus());
        assertTrue(a.isEntryAuthorized());
        assertEquals(validFrom, a.getValidFrom());
        assertEquals(validUntil, a.getValidUntil());
        assertEquals("PROPIETARIO", a.getAssignmentType());
        assertEquals("CLIENTE", a.getRolAutorizacion());
    }
}
