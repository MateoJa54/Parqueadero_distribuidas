package ec.edu.espe.usuarios.services.Impl;

import ec.edu.espe.usuarios.audit.AuditPublisher;
import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioUpdateDto;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServicioImplTest {

    private UsuarioRepositorio usuarioRepositorio;
    private PersonaRepositorio personaRepositorio;
    private UsuarioRolRepositorio usuarioRolRepositorio;
    private UtilMappers mapper;
    private AuditPublisher auditPublisher;
    private UsuarioServicioImpl servicio;

    @BeforeEach
    void setUp() {
        usuarioRepositorio = mock(UsuarioRepositorio.class);
        personaRepositorio = mock(PersonaRepositorio.class);
        usuarioRolRepositorio = mock(UsuarioRolRepositorio.class);
        mapper = mock(UtilMappers.class);
        auditPublisher = mock(AuditPublisher.class);
        servicio = new UsuarioServicioImpl(
                usuarioRepositorio, personaRepositorio,
                usuarioRolRepositorio, mapper, auditPublisher);
    }

    // --- helpers ---

    private Persona persona(UUID id, boolean active) {
        return Persona.builder()
                .id(id)
                .firstName("Juan")
                .lastName("Perez")
                .active(active)
                .build();
    }

    private Usuario usuario(UUID id, Persona persona, boolean active) {
        return Usuario.builder()
                .persona(persona)
                .username("juanp")
                .passwordHash("hashed")
                .active(active)
                .build();
    }

    private UsuarioResponseDto responseDto(UUID personaId) {
        return UsuarioResponseDto.builder()
                .id(personaId)
                .username("juanp")
                .active(true)
                .build();
    }

    // --- listarUsuarios ---

    @Test
    @DisplayName("listarUsuarios mapea todos los usuarios")
    void listarUsuariosOk() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);
        when(usuarioRepositorio.findAll()).thenReturn(List.of(u));
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        List<UsuarioResponseDto> lista = servicio.listarUsuarios();

        assertEquals(1, lista.size());
    }

    // --- obtenerUsuario ---

    @Test
    @DisplayName("obtenerUsuario devuelve usuario existente")
    void obtenerUsuarioExiste() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);
        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        UsuarioResponseDto dto = servicio.obtenerUsuario(id);
        assertEquals(id, dto.getId());
    }

    @Test
    @DisplayName("obtenerUsuario lanza RNE cuando no existe")
    void obtenerUsuarioNoExiste() {
        UUID id = UUID.randomUUID();
        when(usuarioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerUsuario(id));
    }

    // --- crearUsuario ---

    @Test
    @DisplayName("crearUsuario happy path crea y guarda correctamente")
    void crearUsuarioOk() {
        UUID personaId = UUID.randomUUID();
        Persona p = persona(personaId, true);
        Usuario u = usuario(personaId, p, true);

        UsuarioRequestDto request = new UsuarioRequestDto();
        request.setIdPersona(personaId);
        request.setUsername("juanp");
        request.setPassword("Passw0rd");

        when(personaRepositorio.findById(personaId)).thenReturn(Optional.of(p));
        when(usuarioRepositorio.existsById(personaId)).thenReturn(false);
        when(usuarioRepositorio.existsByUsernameIgnoreCase("juanp")).thenReturn(false);
        when(usuarioRepositorio.save(any())).thenReturn(u);
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(personaId));

        UsuarioResponseDto dto = servicio.crearUsuario(request);

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("CREATE"), eq("USUARIO"), any());
    }

    @Test
    @DisplayName("crearUsuario lanza RNE si persona no existe")
    void crearUsuarioPersonaNoExiste() {
        UUID personaId = UUID.randomUUID();
        UsuarioRequestDto request = new UsuarioRequestDto();
        request.setIdPersona(personaId);

        when(personaRepositorio.findById(personaId)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crearUsuario(request));
    }

    @Test
    @DisplayName("crearUsuario lanza RNE si persona esta inactiva")
    void crearUsuarioPersonaInactiva() {
        UUID personaId = UUID.randomUUID();
        Persona p = persona(personaId, false);
        UsuarioRequestDto request = new UsuarioRequestDto();
        request.setIdPersona(personaId);

        when(personaRepositorio.findById(personaId)).thenReturn(Optional.of(p));

        assertThrows(ReglaNegocioException.class, () -> servicio.crearUsuario(request));
    }

    @Test
    @DisplayName("crearUsuario lanza RNE si persona ya tiene usuario")
    void crearUsuarioPersonaYaTieneUsuario() {
        UUID personaId = UUID.randomUUID();
        Persona p = persona(personaId, true);
        UsuarioRequestDto request = new UsuarioRequestDto();
        request.setIdPersona(personaId);

        when(personaRepositorio.findById(personaId)).thenReturn(Optional.of(p));
        when(usuarioRepositorio.existsById(personaId)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearUsuario(request));
    }

    @Test
    @DisplayName("crearUsuario lanza RNE si username ya existe")
    void crearUsuarioUsernameRepetido() {
        UUID personaId = UUID.randomUUID();
        Persona p = persona(personaId, true);
        UsuarioRequestDto request = new UsuarioRequestDto();
        request.setIdPersona(personaId);
        request.setUsername("juanp");

        when(personaRepositorio.findById(personaId)).thenReturn(Optional.of(p));
        when(usuarioRepositorio.existsById(personaId)).thenReturn(false);
        when(usuarioRepositorio.existsByUsernameIgnoreCase("juanp")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearUsuario(request));
    }

    // --- actualizarUsuario ---

    @Test
    @DisplayName("actualizarUsuario happy path actualiza username")
    void actualizarUsuarioOk() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);

        UsuarioUpdateDto request = new UsuarioUpdateDto();
        request.setIdPersona(id);
        request.setUsername("nuevousr");

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));
        when(usuarioRepositorio.existsByUsernameIgnoreCaseAndIdNot("nuevousr", id)).thenReturn(false);
        when(usuarioRepositorio.save(any())).thenReturn(u);
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        UsuarioResponseDto dto = servicio.actualizarUsuario(id, request);

        assertNotNull(dto);
        assertEquals("nuevousr", u.getUsername());
        verify(auditPublisher).publicar(eq("UPDATE"), eq("USUARIO"), any());
    }

    @Test
    @DisplayName("actualizarUsuario actualiza password si viene no vacio")
    void actualizarUsuarioConPassword() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);
        String oldHash = u.getPasswordHash();

        UsuarioUpdateDto request = new UsuarioUpdateDto();
        request.setIdPersona(id);
        request.setUsername("juanp");
        request.setPassword("NewPass1");

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));
        when(usuarioRepositorio.existsByUsernameIgnoreCaseAndIdNot("juanp", id)).thenReturn(false);
        when(usuarioRepositorio.save(any())).thenReturn(u);
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        servicio.actualizarUsuario(id, request);

        assertNotEquals(oldHash, u.getPasswordHash());
    }

    @Test
    @DisplayName("actualizarUsuario lanza RNE si no existe")
    void actualizarUsuarioNoExiste() {
        UUID id = UUID.randomUUID();
        when(usuarioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizarUsuario(id, new UsuarioUpdateDto()));
    }

    @Test
    @DisplayName("actualizarUsuario lanza IAE si cambia la persona")
    void actualizarUsuarioCambiaPersona() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);

        UsuarioUpdateDto request = new UsuarioUpdateDto();
        request.setIdPersona(UUID.randomUUID()); // otro id
        request.setUsername("juanp");

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarUsuario(id, request));
    }

    @Test
    @DisplayName("actualizarUsuario lanza RNE si username ya existe en otro usuario")
    void actualizarUsuarioUsernameRepetido() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);

        UsuarioUpdateDto request = new UsuarioUpdateDto();
        request.setIdPersona(id);
        request.setUsername("ocupado");

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));
        when(usuarioRepositorio.existsByUsernameIgnoreCaseAndIdNot("ocupado", id)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizarUsuario(id, request));
    }

    // --- activarUsuario ---

    @Test
    @DisplayName("activarUsuario activa al usuario")
    void activarUsuarioOk() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, false);

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));
        when(usuarioRepositorio.save(any())).thenReturn(u);
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        servicio.activarUsuario(id);

        assertTrue(u.isActive());
        verify(auditPublisher).publicar(eq("UPDATE"), eq("USUARIO"), any());
    }

    @Test
    @DisplayName("activarUsuario lanza RNE si la persona esta inactiva")
    void activarUsuarioPersonaInactiva() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, false);
        Usuario u = usuario(id, p, false);

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));

        assertThrows(ReglaNegocioException.class, () -> servicio.activarUsuario(id));
    }

    @Test
    @DisplayName("activarUsuario lanza RNE si no existe")
    void activarUsuarioNoExiste() {
        UUID id = UUID.randomUUID();
        when(usuarioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.activarUsuario(id));
    }

    // --- desactivarUsuario ---

    @Test
    @DisplayName("desactivarUsuario desactiva usuario y revoca roles activos")
    void desactivarUsuarioOk() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);
        UsuarioRol rol = UsuarioRol.builder().active(true).build();

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));
        when(usuarioRolRepositorio.findByUsuario(u)).thenReturn(List.of(rol));
        when(usuarioRepositorio.save(any())).thenReturn(u);
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        servicio.desactivarUsuario(id);

        assertFalse(u.isActive());
        assertFalse(rol.isActive());
        verify(usuarioRolRepositorio).save(rol);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("USUARIO"), any());
    }

    @Test
    @DisplayName("desactivarUsuario no revoca roles ya inactivos")
    void desactivarUsuarioNoRevocaRolesInactivos() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);
        UsuarioRol rol = UsuarioRol.builder().active(false).build();

        when(usuarioRepositorio.findById(id)).thenReturn(Optional.of(u));
        when(usuarioRolRepositorio.findByUsuario(u)).thenReturn(List.of(rol));
        when(usuarioRepositorio.save(any())).thenReturn(u);
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        servicio.desactivarUsuario(id);

        verify(usuarioRolRepositorio, never()).save(rol);
    }

    @Test
    @DisplayName("desactivarUsuario lanza RNE si no existe")
    void desactivarUsuarioNoExiste() {
        UUID id = UUID.randomUUID();
        when(usuarioRepositorio.findById(id)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.desactivarUsuario(id));
    }

    // --- buscarUsuarios ---

    @Test
    @DisplayName("buscarUsuarios devuelve resultados por username")
    void buscarUsuariosOk() {
        UUID id = UUID.randomUUID();
        Persona p = persona(id, true);
        Usuario u = usuario(id, p, true);
        when(usuarioRepositorio.findByUsernameContainingIgnoreCase("juan")).thenReturn(List.of(u));
        when(mapper.toUsuarioResponse(u)).thenReturn(responseDto(id));

        List<UsuarioResponseDto> lista = servicio.buscarUsuarios("juan");

        assertEquals(1, lista.size());
    }

    @Test
    @DisplayName("buscarUsuarios lanza IAE si username es vacio")
    void buscarUsuariosVacio() {
        assertThrows(IllegalArgumentException.class, () -> servicio.buscarUsuarios(""));
        assertThrows(IllegalArgumentException.class, () -> servicio.buscarUsuarios(null));
    }
}
