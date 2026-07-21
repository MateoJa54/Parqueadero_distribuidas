package ec.edu.espe.usuarios.services.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ec.edu.espe.usuarios.audit.AuditPublisher;
import ec.edu.espe.usuarios.dtos.RolRequestDto;
import ec.edu.espe.usuarios.dtos.RolResponseDto;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;

class RolServicioImplTest {

    private RolRepositorio rolRepositorio;
    private UsuarioRolRepositorio usuarioRolRepositorio;
    private AuditPublisher auditPublisher;
    private RolServicioImpl servicio;

    private UUID idRol;
    private Rol rol;

    @BeforeEach
    void setUp() {
        rolRepositorio = mock(RolRepositorio.class);
        usuarioRolRepositorio = mock(UsuarioRolRepositorio.class);
        auditPublisher = mock(AuditPublisher.class);
        UtilMappers mapper = new UtilMappers();

        servicio = new RolServicioImpl(rolRepositorio, usuarioRolRepositorio, mapper, auditPublisher);

        idRol = UUID.randomUUID();
        rol = Rol.builder()
                .id(idRol)
                .name("ADMIN")
                .description("Administrador")
                .active(true)
                .build();
    }

    private RolRequestDto request(String nombre) {
        return RolRequestDto.builder().name(nombre).description("desc").build();
    }

    // ---- listarRoles ----

    @Test
    @DisplayName("listarRoles retorna lista mapeada")
    void listarRolesOk() {
        when(rolRepositorio.findAll()).thenReturn(List.of(rol));

        List<RolResponseDto> resultado = servicio.listarRoles();

        assertEquals(1, resultado.size());
        assertEquals("ADMIN", resultado.get(0).getName());
    }

    // ---- obtenerRol ----

    @Test
    @DisplayName("obtenerRol retorna dto cuando existe")
    void obtenerRolExistente() {
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));

        RolResponseDto dto = servicio.obtenerRol(idRol);

        assertEquals(idRol, dto.getId());
    }

    @Test
    @DisplayName("obtenerRol lanza RecursoNoEncontrado cuando no existe")
    void obtenerRolNoExistente() {
        when(rolRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerRol(idRol));
    }

    // ---- crearRol ----

    @Test
    @DisplayName("crearRol normaliza nombre a mayusculas y lo guarda")
    void crearRolOk() {
        when(rolRepositorio.existsByName("ADMIN")).thenReturn(false);
        when(rolRepositorio.save(any(Rol.class))).thenReturn(rol);

        RolResponseDto dto = servicio.crearRol(request(" admin "));

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("CREATE"), eq("ROL"), any(Rol.class));
    }

    @Test
    @DisplayName("crearRol lanza ReglaNegocio si nombre ya existe")
    void crearRolNombreDuplicado() {
        when(rolRepositorio.existsByName("ADMIN")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearRol(request("admin")));
        verify(rolRepositorio, never()).save(any());
    }

    // ---- actualizarRol ----

    @Test
    @DisplayName("actualizarRol actualiza nombre y publica UPDATE")
    void actualizarRolOk() {
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(rolRepositorio.existsByNameAndIdNot("SUPERVISOR", idRol)).thenReturn(false);
        when(rolRepositorio.save(any(Rol.class))).thenReturn(rol);

        RolResponseDto dto = servicio.actualizarRol(idRol, request("supervisor"));

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ROL"), any(Rol.class));
    }

    @Test
    @DisplayName("actualizarRol lanza RecursoNoEncontrado si no existe")
    void actualizarRolNoExistente() {
        when(rolRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizarRol(idRol, request("nuevo")));
    }

    @Test
    @DisplayName("actualizarRol lanza ReglaNegocio si nombre colisiona con otro")
    void actualizarRolNombreDuplicado() {
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(rolRepositorio.existsByNameAndIdNot("OTRO", idRol)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarRol(idRol, request("otro")));
        verify(rolRepositorio, never()).save(any());
    }

    // ---- activarRol ----

    @Test
    @DisplayName("activarRol pone active=true y publica UPDATE")
    void activarRolOk() {
        rol.setActive(false);
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(rolRepositorio.save(any(Rol.class))).thenReturn(rol);

        RolResponseDto dto = servicio.activarRol(idRol);

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ROL"), any(Rol.class));
    }

    @Test
    @DisplayName("activarRol lanza RecursoNoEncontrado si no existe")
    void activarRolNoExistente() {
        when(rolRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.activarRol(idRol));
    }

    // ---- desactivarRol ----

    @Test
    @DisplayName("desactivarRol cuando no hay usuarios activos desactiva el rol")
    void desactivarRolSinUsuariosActivos() {
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(usuarioRolRepositorio.findByRol(rol)).thenReturn(List.of());
        when(rolRepositorio.save(any(Rol.class))).thenReturn(rol);

        RolResponseDto dto = servicio.desactivarRol(idRol);

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ROL"), any(Rol.class));
    }

    @Test
    @DisplayName("desactivarRol lanza ReglaNegocio si hay usuarios activos")
    void desactivarRolConUsuariosActivos() {
        UsuarioRol asignacionActiva = UsuarioRol.builder().active(true).build();
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(usuarioRolRepositorio.findByRol(rol)).thenReturn(List.of(asignacionActiva));

        assertThrows(ReglaNegocioException.class, () -> servicio.desactivarRol(idRol));
        verify(rolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("desactivarRol permite cuando solo hay asignaciones inactivas")
    void desactivarRolConUsuariosSoloInactivos() {
        UsuarioRol asignacionInactiva = UsuarioRol.builder().active(false).build();
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(usuarioRolRepositorio.findByRol(rol)).thenReturn(List.of(asignacionInactiva));
        when(rolRepositorio.save(any(Rol.class))).thenReturn(rol);

        RolResponseDto dto = servicio.desactivarRol(idRol);

        assertNotNull(dto);
    }

    @Test
    @DisplayName("desactivarRol lanza RecursoNoEncontrado si no existe")
    void desactivarRolNoExistente() {
        when(rolRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.desactivarRol(idRol));
    }
}
