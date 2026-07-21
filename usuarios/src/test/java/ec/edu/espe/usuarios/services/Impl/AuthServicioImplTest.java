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
import ec.edu.espe.usuarios.dtos.AsignarRolRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.dtos.UsuarioRequestDto;
import ec.edu.espe.usuarios.dtos.UsuarioResponseDto;
import ec.edu.espe.usuarios.dtos.auth.AuthResponse;
import ec.edu.espe.usuarios.dtos.auth.LoginRequest;
import ec.edu.espe.usuarios.dtos.auth.PerfilResponse;
import ec.edu.espe.usuarios.dtos.auth.RefreshRequest;
import ec.edu.espe.usuarios.dtos.auth.RegisterRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroClienteRequest;
import ec.edu.espe.usuarios.dtos.auth.RegistroCompletoRequest;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.security.JwtService;
import ec.edu.espe.usuarios.security.RolesBase;
import ec.edu.espe.usuarios.services.AsignacionServicio;
import ec.edu.espe.usuarios.services.PersonaServicio;
import ec.edu.espe.usuarios.services.UsuarioServicio;
import ec.edu.espe.usuarios.utils.CredencialesInvalidasException;
import ec.edu.espe.usuarios.utils.PasswordUtil;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class AuthServicioImplTest {

    private UsuarioRepositorio usuarioRepositorio;
    private UsuarioRolRepositorio usuarioRolRepositorio;
    private RolRepositorio rolRepositorio;
    private PersonaRepositorio personaRepositorio;
    private UsuarioServicio usuarioServicio;
    private PersonaServicio personaServicio;
    private AsignacionServicio asignacionServicio;
    private JwtService jwtService;
    private AuditPublisher auditPublisher;
    private AuthServicioImpl self;
    private AuthServicioImpl servicio;

    private UUID idUsuario;
    private Usuario usuario;
    private Persona persona;
    private Rol rolCliente;

    @BeforeEach
    void setUp() throws Exception {
        usuarioRepositorio = mock(UsuarioRepositorio.class);
        usuarioRolRepositorio = mock(UsuarioRolRepositorio.class);
        rolRepositorio = mock(RolRepositorio.class);
        personaRepositorio = mock(PersonaRepositorio.class);
        usuarioServicio = mock(UsuarioServicio.class);
        personaServicio = mock(PersonaServicio.class);
        asignacionServicio = mock(AsignacionServicio.class);
        jwtService = mock(JwtService.class);
        auditPublisher = mock(AuditPublisher.class);
        self = mock(AuthServicioImpl.class);

        servicio = new AuthServicioImpl(
                usuarioRepositorio, usuarioRolRepositorio, rolRepositorio,
                personaRepositorio, usuarioServicio, personaServicio,
                asignacionServicio, jwtService, auditPublisher);

        // inject self via reflection
        java.lang.reflect.Field selfField = AuthServicioImpl.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(servicio, self);

        idUsuario = UUID.randomUUID();
        persona = Persona.builder()
                .id(idUsuario)
                .firstName("Ana")
                .middleName("Sofia")
                .lastName("Lopez")
                .active(true)
                .build();
        usuario = Usuario.builder()
                .id(idUsuario)
                .username("ana.lopez")
                .passwordHash(PasswordUtil.hash("Password1"))
                .active(true)
                .persona(persona)
                .build();
        rolCliente = Rol.builder()
                .id(UUID.randomUUID())
                .name(RolesBase.CLIENTE)
                .active(true)
                .build();

        when(jwtService.generarToken(any(), anyString(), any())).thenReturn("access-token");
        when(jwtService.generarRefreshToken(any(), anyString())).thenReturn("refresh-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshExpirationSeconds()).thenReturn(86400L);
        when(usuarioRolRepositorio.findByUsuario(any())).thenReturn(List.of());
    }

    // ---- login ----

    @Test
    @DisplayName("login retorna AuthResponse con credenciales validas")
    void loginOk() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        when(usuarioRepositorio.findByUsernameIgnoreCase("ana.lopez")).thenReturn(Optional.of(usuario));
        when(usuarioRepositorio.save(any())).thenReturn(usuario);

        AuthResponse resp = servicio.login(req);

        assertNotNull(resp);
        assertEquals("access-token", resp.getToken());
        assertEquals("refresh-token", resp.getRefreshToken());
        assertEquals(idUsuario, resp.getIdUsuario());
    }

    @Test
    @DisplayName("login lanza CredencialesInvalidas si usuario no existe")
    void loginUsuarioNoExiste() {
        LoginRequest req = new LoginRequest();
        req.setUsername("noexiste");
        req.setPassword("any");

        when(usuarioRepositorio.findByUsernameIgnoreCase("noexiste")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> servicio.login(req));
    }

    @Test
    @DisplayName("login lanza CredencialesInvalidas si usuario inactivo")
    void loginUsuarioInactivo() {
        usuario.setActive(false);
        LoginRequest req = new LoginRequest();
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        when(usuarioRepositorio.findByUsernameIgnoreCase("ana.lopez")).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class, () -> servicio.login(req));
    }

    @Test
    @DisplayName("login lanza CredencialesInvalidas si contrasena incorrecta")
    void loginPasswordIncorrecto() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ana.lopez");
        req.setPassword("WrongPass1");

        when(usuarioRepositorio.findByUsernameIgnoreCase("ana.lopez")).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class, () -> servicio.login(req));
    }

    @Test
    @DisplayName("login incluye rol en auditoria si hay roles activos")
    void loginConRolActivo() {
        UsuarioRol asignacion = UsuarioRol.builder().active(true).rol(rolCliente).build();
        LoginRequest req = new LoginRequest();
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        when(usuarioRepositorio.findByUsernameIgnoreCase("ana.lopez")).thenReturn(Optional.of(usuario));
        when(usuarioRepositorio.save(any())).thenReturn(usuario);
        when(usuarioRolRepositorio.findByUsuario(usuario)).thenReturn(List.of(asignacion));

        AuthResponse resp = servicio.login(req);

        assertNotNull(resp);
        assertTrue(resp.getRoles().contains(RolesBase.CLIENTE));
    }

    // ---- register ----

    @Test
    @DisplayName("register crea usuario y asigna rol CLIENTE")
    void registerOk() {
        RegisterRequest req = new RegisterRequest();
        req.setIdPersona(idUsuario);
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        UsuarioResponseDto creado = UsuarioResponseDto.builder().id(idUsuario).build();
        when(usuarioServicio.crearUsuario(any(UsuarioRequestDto.class))).thenReturn(creado);
        when(rolRepositorio.findByName(RolesBase.CLIENTE)).thenReturn(Optional.of(rolCliente));
        when(asignacionServicio.asignarRol(any(AsignarRolRequestDto.class))).thenReturn(null);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));

        AuthResponse resp = servicio.register(req);

        assertNotNull(resp);
        assertEquals("access-token", resp.getToken());
        verify(asignacionServicio).asignarRol(any(AsignarRolRequestDto.class));
    }

    @Test
    @DisplayName("register lanza ReglaNegocio si rol CLIENTE no existe")
    void registerSinRolCliente() {
        RegisterRequest req = new RegisterRequest();
        req.setIdPersona(idUsuario);
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        UsuarioResponseDto creado = UsuarioResponseDto.builder().id(idUsuario).build();
        when(usuarioServicio.crearUsuario(any())).thenReturn(creado);
        when(rolRepositorio.findByName(RolesBase.CLIENTE)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> servicio.register(req));
    }

    @Test
    @DisplayName("register lanza RecursoNoEncontrado si usuario no persiste")
    void registerUsuarioNoEncontradoTrasCreacion() {
        RegisterRequest req = new RegisterRequest();
        req.setIdPersona(idUsuario);
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        UsuarioResponseDto creado = UsuarioResponseDto.builder().id(idUsuario).build();
        when(usuarioServicio.crearUsuario(any())).thenReturn(creado);
        when(rolRepositorio.findByName(RolesBase.CLIENTE)).thenReturn(Optional.of(rolCliente));
        when(asignacionServicio.asignarRol(any())).thenReturn(null);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.register(req));
    }

    // ---- registrarCliente ----

    @Test
    @DisplayName("registrarCliente delega a register cuando identidad es valida")
    void registrarClienteOk() {
        RegistroClienteRequest req = new RegistroClienteRequest();
        req.setDni("1713175071");
        req.setEmail("ana@example.com");
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        persona.setEmail("ana@example.com");
        when(personaRepositorio.findByDni("1713175071")).thenReturn(Optional.of(persona));
        when(usuarioRepositorio.existsById(idUsuario)).thenReturn(false);

        AuthResponse fakeResp = AuthResponse.builder().token("t").build();
        when(self.register(any(RegisterRequest.class))).thenReturn(fakeResp);

        AuthResponse resp = servicio.registrarCliente(req);

        assertNotNull(resp);
        verify(self).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("registrarCliente lanza ReglaNegocio si dni no existe")
    void registrarClienteDniInexistente() {
        RegistroClienteRequest req = new RegistroClienteRequest();
        req.setDni("9999999999");
        req.setEmail("x@x.com");
        req.setUsername("user");
        req.setPassword("Pass1word");

        when(personaRepositorio.findByDni("9999999999")).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCliente(req));
    }

    @Test
    @DisplayName("registrarCliente lanza ReglaNegocio si email no coincide")
    void registrarClienteEmailNoCoincide() {
        RegistroClienteRequest req = new RegistroClienteRequest();
        req.setDni("1713175071");
        req.setEmail("otro@example.com");
        req.setUsername("user");
        req.setPassword("Pass1word");

        persona.setEmail("real@example.com");
        when(personaRepositorio.findByDni("1713175071")).thenReturn(Optional.of(persona));

        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCliente(req));
    }

    @Test
    @DisplayName("registrarCliente lanza ReglaNegocio si persona inactiva")
    void registrarClientePersonaInactiva() {
        RegistroClienteRequest req = new RegistroClienteRequest();
        req.setDni("1713175071");
        req.setEmail("ana@example.com");
        req.setUsername("user");
        req.setPassword("Pass1word");

        persona.setEmail("ana@example.com");
        persona.setActive(false);
        when(personaRepositorio.findByDni("1713175071")).thenReturn(Optional.of(persona));

        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCliente(req));
    }

    @Test
    @DisplayName("registrarCliente lanza ReglaNegocio si ya tiene usuario")
    void registrarClienteYaTieneUsuario() {
        RegistroClienteRequest req = new RegistroClienteRequest();
        req.setDni("1713175071");
        req.setEmail("ana@example.com");
        req.setUsername("user");
        req.setPassword("Pass1word");

        persona.setEmail("ana@example.com");
        when(personaRepositorio.findByDni("1713175071")).thenReturn(Optional.of(persona));
        when(usuarioRepositorio.existsById(idUsuario)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.registrarCliente(req));
    }

    // ---- registrarCompleto ----

    @Test
    @DisplayName("registrarCompleto crea persona y delega a register")
    void registrarCompletoOk() {
        RegistroCompletoRequest req = new RegistroCompletoRequest();
        req.setFirstName("Ana");
        req.setLastName("Lopez");
        req.setDni("1713175071");
        req.setEmail("ana@example.com");
        req.setPhone("0991234567");
        req.setNationality("Ecuatoriana");
        req.setUsername("ana.lopez");
        req.setPassword("Password1");

        PersonaResponseDto personaDto = PersonaResponseDto.builder().id(idUsuario).build();
        when(personaServicio.crearPersona(any())).thenReturn(personaDto);

        AuthResponse fakeResp = AuthResponse.builder().token("t").build();
        when(self.register(any(RegisterRequest.class))).thenReturn(fakeResp);

        AuthResponse resp = servicio.registrarCompleto(req);

        assertNotNull(resp);
        verify(personaServicio).crearPersona(any());
        verify(self).register(any(RegisterRequest.class));
    }

    // ---- refrescar ----

    @Test
    @DisplayName("refrescar emite nuevo access con refresh valido")
    void refrescarOk() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("valid-refresh");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(idUsuario.toString());
        when(jwtService.validarTipo("valid-refresh", JwtService.TYPE_REFRESH)).thenReturn(claims);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));

        AuthResponse resp = servicio.refrescar(req);

        assertNotNull(resp);
        assertEquals("access-token", resp.getToken());
    }

    @Test
    @DisplayName("refrescar lanza CredencialesInvalidas si token invalido")
    void refrescarTokenInvalido() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("bad-token");

        when(jwtService.validarTipo(anyString(), eq(JwtService.TYPE_REFRESH)))
                .thenThrow(new JwtException("invalid"));

        assertThrows(CredencialesInvalidasException.class, () -> servicio.refrescar(req));
    }

    @Test
    @DisplayName("refrescar lanza CredencialesInvalidas si subject no es UUID")
    void refrescarSubjectInvalido() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("not-a-uuid");
        when(jwtService.validarTipo("token", JwtService.TYPE_REFRESH)).thenReturn(claims);

        assertThrows(CredencialesInvalidasException.class, () -> servicio.refrescar(req));
    }

    @Test
    @DisplayName("refrescar lanza CredencialesInvalidas si usuario no existe")
    void refrescarUsuarioNoExiste() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(idUsuario.toString());
        when(jwtService.validarTipo("token", JwtService.TYPE_REFRESH)).thenReturn(claims);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> servicio.refrescar(req));
    }

    @Test
    @DisplayName("refrescar lanza CredencialesInvalidas si usuario inactivo")
    void refrescarUsuarioInactivo() {
        usuario.setActive(false);
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("token");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(idUsuario.toString());
        when(jwtService.validarTipo("token", JwtService.TYPE_REFRESH)).thenReturn(claims);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class, () -> servicio.refrescar(req));
    }

    // ---- perfil ----

    @Test
    @DisplayName("perfil retorna datos del usuario con nombreCompleto incluyendo middleName")
    void perfilConMiddleName() {
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));

        PerfilResponse resp = servicio.perfil(idUsuario);

        assertEquals(idUsuario, resp.getIdUsuario());
        assertEquals("ana.lopez", resp.getUsername());
        assertTrue(resp.getNombreCompleto().contains("Sofia"));
    }

    @Test
    @DisplayName("perfil retorna nombreCompleto sin middleName cuando es blank")
    void perfilSinMiddleName() {
        persona.setMiddleName(null);
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));

        PerfilResponse resp = servicio.perfil(idUsuario);

        assertEquals("Ana Lopez", resp.getNombreCompleto());
    }

    @Test
    @DisplayName("perfil retorna nombreCompleto sin middleName cuando es espacio en blanco")
    void perfilMiddleNameBlank() {
        persona.setMiddleName("   ");
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));

        PerfilResponse resp = servicio.perfil(idUsuario);

        assertEquals("Ana Lopez", resp.getNombreCompleto());
    }

    @Test
    @DisplayName("perfil lanza RecursoNoEncontrado si usuario no existe")
    void perfilNoExiste() {
        when(usuarioRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.perfil(idUsuario));
    }

    @Test
    @DisplayName("perfil incluye roles activos del usuario")
    void perfilConRoles() {
        UsuarioRol asignacion = UsuarioRol.builder().active(true).rol(rolCliente).build();
        when(usuarioRepositorio.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(usuarioRolRepositorio.findByUsuario(usuario)).thenReturn(List.of(asignacion));

        PerfilResponse resp = servicio.perfil(idUsuario);

        assertTrue(resp.getRoles().contains(RolesBase.CLIENTE));
    }
}
