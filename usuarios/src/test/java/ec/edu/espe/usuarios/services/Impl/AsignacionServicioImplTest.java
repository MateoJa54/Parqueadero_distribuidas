package ec.edu.espe.usuarios.services.Impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ec.edu.espe.usuarios.audit.AuditPublisher;
import ec.edu.espe.usuarios.dtos.AsignacionResponseDto;
import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.entidades.UsuarioRolId;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;

@ExtendWith(MockitoExtension.class)
class AsignacionServicioImplTest {

    @Mock
    private UsuarioRolRepositorio usuarioRolRepositorio;
    @Mock
    private UsuarioRepositorio usuarioRepositorio;
    @Mock
    private RolRepositorio rolRepositorio;
    @Mock
    private AuditPublisher auditPublisher;

    // Mapper real: es logica pura sin dependencias externas, asi cubrimos tambien UtilMappers.
    private final UtilMappers mapper = new UtilMappers();

    @InjectMocks
    private AsignacionServicioImpl servicio;

    private UUID idUsuario;
    private UUID idRol;
    private Usuario usuario;
    private Rol rol;

    @BeforeEach
    void setUp() {
        servicio = new AsignacionServicioImpl(usuarioRolRepositorio, usuarioRepositorio,
                rolRepositorio, mapper, auditPublisher);

        idUsuario = UUID.randomUUID();
        idRol = UUID.randomUUID();

        usuario = Usuario.builder()
                .id(idUsuario)
                .username("jdoe")
                .active(true)
                .build();

        rol = Rol.builder()
                .id(idRol)
                .name("ADMIN")
                .active(true)
                .build();
    }

    private UsuarioRol nuevaAsignacion(boolean active) {
        return UsuarioRol.builder()
                .id(new UsuarioRolId(idUsuario, idRol))
                .usuario(usuario)
                .rol(rol)
                .active(active)
                .build();
    }

    private AsignarRolRequestDto request() {
        return AsignarRolRequestDto.builder().idUser(idUsuario).idRole(idRol).build();
    }

    @Test
    @DisplayName("asignarRol asigna correctamente cuando todo es valido")
    void asignarRolExitoso() {
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(usuarioRolRepositorio.existsByUsuarioAndRol(usuario, rol)).thenReturn(false);
        when(usuarioRolRepositorio.save(any(UsuarioRol.class))).thenAnswer(inv -> inv.getArgument(0));

        AsignacionResponseDto respuesta = servicio.asignarRol(request());

        assertNotNull(respuesta);
        assertEquals(idUsuario, respuesta.getIdUser());
        assertEquals(idRol, respuesta.getIdRole());
        assertEquals("ADMIN", respuesta.getRol());
        assertEquals("jdoe", respuesta.getUsername());
        verify(usuarioRolRepositorio).save(any(UsuarioRol.class));
        verify(auditPublisher).publicar(eq("CREATE"), eq("ASIGNACION-ROL"), any(UsuarioRol.class));
    }

    @Test
    @DisplayName("asignarRol lanza RecursoNoEncontrado si el usuario no existe")
    void asignarRolUsuarioInexistente() {
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.empty());

        var req = request();
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.asignarRol(req));
        verify(usuarioRolRepositorio, never()).save(any());
        verify(auditPublisher, never()).publicar(any(), any(), any());
    }

    @Test
    @DisplayName("asignarRol lanza RecursoNoEncontrado si el rol no existe")
    void asignarRolRolInexistente() {
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.empty());

        var req = request();
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.asignarRol(req));
        verify(usuarioRolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("asignarRol lanza ReglaNegocio si el rol esta inactivo")
    void asignarRolRolInactivo() {
        rol.setActive(false);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));

        var req = request();
        assertThrows(ReglaNegocioException.class, () -> servicio.asignarRol(req));
        verify(usuarioRolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("asignarRol lanza ReglaNegocio si el usuario esta inactivo")
    void asignarRolUsuarioInactivo() {
        usuario.setActive(false);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));

        var req = request();
        assertThrows(ReglaNegocioException.class, () -> servicio.asignarRol(req));
        verify(usuarioRolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("asignarRol lanza ReglaNegocio si la asignacion ya existe")
    void asignarRolYaAsignado() {
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(rolRepositorio.findById(idRol)).thenReturn(Optional.of(rol));
        when(usuarioRolRepositorio.existsByUsuarioAndRol(usuario, rol)).thenReturn(true);

        var req = request();
        assertThrows(ReglaNegocioException.class, () -> servicio.asignarRol(req));
        verify(usuarioRolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("listarAsignaciones mapea todas las asignaciones")
    void listarAsignaciones() {
        when(usuarioRolRepositorio.findAll()).thenReturn(List.of(nuevaAsignacion(true)));

        List<AsignacionResponseDto> resultado = servicio.listarAsignaciones();

        assertEquals(1, resultado.size());
        assertEquals("ADMIN", resultado.get(0).getRol());
    }

    @Test
    @DisplayName("listarRolesDeUsuario devuelve los roles del usuario existente")
    void listarRolesDeUsuario() {
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepositorio.findByUsuario(usuario)).thenReturn(List.of(nuevaAsignacion(true)));

        List<AsignacionResponseDto> resultado = servicio.listarRolesDeUsuario(idUsuario);

        assertEquals(1, resultado.size());
        assertEquals(idUsuario, resultado.get(0).getIdUser());
    }

    @Test
    @DisplayName("listarRolesDeUsuario lanza RecursoNoEncontrado si el usuario no existe")
    void listarRolesDeUsuarioInexistente() {
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.listarRolesDeUsuario(idUsuario));
    }

    @Test
    @DisplayName("desactivarAsignacion marca inactiva y publica UPDATE")
    void desactivarAsignacion() {
        UsuarioRol asignacion = nuevaAsignacion(true);
        when(usuarioRolRepositorio.findById(any(UsuarioRolId.class))).thenReturn(Optional.of(asignacion));
        when(usuarioRolRepositorio.save(any(UsuarioRol.class))).thenAnswer(inv -> inv.getArgument(0));

        AsignacionResponseDto respuesta = servicio.desactivarAsignacion(idUsuario, idRol);

        assertFalse(respuesta.isActive());
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ASIGNACION-ROL"), any(UsuarioRol.class));
    }

    @Test
    @DisplayName("desactivarAsignacion lanza RecursoNoEncontrado si no existe")
    void desactivarAsignacionInexistente() {
        when(usuarioRolRepositorio.findById(any(UsuarioRolId.class))).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.desactivarAsignacion(idUsuario, idRol));
        verify(usuarioRolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("activarAsignacion reactiva cuando rol y usuario estan activos")
    void activarAsignacionExitoso() {
        UsuarioRol asignacion = nuevaAsignacion(false);
        when(usuarioRolRepositorio.findById(any(UsuarioRolId.class))).thenReturn(Optional.of(asignacion));
        when(usuarioRolRepositorio.save(any(UsuarioRol.class))).thenAnswer(inv -> inv.getArgument(0));

        AsignacionResponseDto respuesta = servicio.activarAsignacion(idUsuario, idRol);

        assertTrue(respuesta.isActive());
        verify(auditPublisher).publicar(eq("UPDATE"), eq("ASIGNACION-ROL"), any(UsuarioRol.class));
    }

    @Test
    @DisplayName("activarAsignacion lanza RecursoNoEncontrado si no existe")
    void activarAsignacionInexistente() {
        when(usuarioRolRepositorio.findById(any(UsuarioRolId.class))).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.activarAsignacion(idUsuario, idRol));
    }

    @Test
    @DisplayName("activarAsignacion lanza ReglaNegocio si el rol esta inactivo")
    void activarAsignacionRolInactivo() {
        rol.setActive(false);
        UsuarioRol asignacion = nuevaAsignacion(false);
        when(usuarioRolRepositorio.findById(any(UsuarioRolId.class))).thenReturn(Optional.of(asignacion));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.activarAsignacion(idUsuario, idRol));
        verify(usuarioRolRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("activarAsignacion lanza ReglaNegocio si el usuario esta inactivo")
    void activarAsignacionUsuarioInactivo() {
        usuario.setActive(false);
        UsuarioRol asignacion = nuevaAsignacion(false);
        when(usuarioRolRepositorio.findById(any(UsuarioRolId.class))).thenReturn(Optional.of(asignacion));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.activarAsignacion(idUsuario, idRol));
        verify(usuarioRolRepositorio, never()).save(any());
    }
}
