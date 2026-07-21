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
import ec.edu.espe.usuarios.dtos.PersonaRequestDto;
import ec.edu.espe.usuarios.dtos.PersonaResponseDto;
import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.services.UsuarioServicio;
import ec.edu.espe.usuarios.utils.RecursoNoEncontradoException;
import ec.edu.espe.usuarios.utils.ReglaNegocioException;
import ec.edu.espe.usuarios.utils.UtilMappers;

class PersonaServicioImplTest {

    private PersonaRepositorio personaRepositorio;
    private UsuarioRepositorio usuarioRepositorio;
    private UsuarioServicio usuarioServicio;
    private AuditPublisher auditPublisher;
    private PersonaServicioImpl servicio;

    private UUID idPersona;
    private Persona persona;

    @BeforeEach
    void setUp() {
        personaRepositorio = mock(PersonaRepositorio.class);
        usuarioRepositorio = mock(UsuarioRepositorio.class);
        usuarioServicio = mock(UsuarioServicio.class);
        auditPublisher = mock(AuditPublisher.class);
        UtilMappers mapper = new UtilMappers();

        servicio = new PersonaServicioImpl(personaRepositorio, usuarioRepositorio, usuarioServicio,
                mapper, auditPublisher);

        idPersona = UUID.randomUUID();
        persona = Persona.builder()
                .id(idPersona)
                .firstName("Juan")
                .middleName("Carlos")
                .lastName("Perez")
                .dni("1713175071")
                .email("juan@example.com")
                .phone("0991234567")
                .address("Quito")
                .nationality("Ecuatoriana")
                .active(true)
                .build();
    }

    private PersonaRequestDto requestValido() {
        return PersonaRequestDto.builder()
                .firstName("Juan")
                .middleName("Carlos")
                .lastName("Perez")
                .dni("1713175071")
                .email("Juan@Example.com")
                .phone("0991234567")
                .address("Quito")
                .nationality("Ecuatoriana")
                .build();
    }

    // ---- listarPersonas ----

    @Test
    @DisplayName("listarPersonas retorna lista mapeada")
    void listarPersonasOk() {
        when(personaRepositorio.findAll()).thenReturn(List.of(persona));

        List<PersonaResponseDto> resultado = servicio.listarPersonas();

        assertEquals(1, resultado.size());
        assertEquals("Juan", resultado.get(0).getFirstName());
    }

    @Test
    @DisplayName("listarPersonas retorna lista vacia")
    void listarPersonasVacia() {
        when(personaRepositorio.findAll()).thenReturn(List.of());

        assertTrue(servicio.listarPersonas().isEmpty());
    }

    // ---- obtenerPersona ----

    @Test
    @DisplayName("obtenerPersona retorna dto cuando existe")
    void obtenerPersonaExistente() {
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));

        PersonaResponseDto dto = servicio.obtenerPersona(idPersona);

        assertEquals(idPersona, dto.getId());
        assertEquals("Perez", dto.getLastName());
    }

    @Test
    @DisplayName("obtenerPersona lanza RecursoNoEncontrado cuando no existe")
    void obtenerPersonaNoExistente() {
        when(personaRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerPersona(idPersona));
    }

    // ---- crearPersona ----

    @Test
    @DisplayName("crearPersona crea y normaliza email a minusculas")
    void crearPersonaOk() {
        when(personaRepositorio.existsByDni(anyString())).thenReturn(false);
        when(personaRepositorio.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(personaRepositorio.existsByPhone(anyString())).thenReturn(false);
        when(personaRepositorio.save(any(Persona.class))).thenReturn(persona);

        PersonaResponseDto dto = servicio.crearPersona(requestValido());

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("CREATE"), eq("PERSONA"), any(Persona.class));
    }

    @Test
    @DisplayName("crearPersona lanza ReglaNegocio si dni ya existe")
    void crearPersonaDniDuplicado() {
        when(personaRepositorio.existsByDni(anyString())).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearPersona(requestValido()));
        verify(personaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("crearPersona lanza ReglaNegocio si email ya existe")
    void crearPersonaEmailDuplicado() {
        when(personaRepositorio.existsByDni(anyString())).thenReturn(false);
        when(personaRepositorio.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearPersona(requestValido()));
        verify(personaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("crearPersona lanza ReglaNegocio si telefono ya existe")
    void crearPersonaTelefonoDuplicado() {
        when(personaRepositorio.existsByDni(anyString())).thenReturn(false);
        when(personaRepositorio.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(personaRepositorio.existsByPhone(anyString())).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.crearPersona(requestValido()));
        verify(personaRepositorio, never()).save(any());
    }

    // ---- actualizarPersona ----

    @Test
    @DisplayName("actualizarPersona actualiza campos y publica UPDATE")
    void actualizarPersonaOk() {
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));
        when(personaRepositorio.existsByDniAndIdNot(anyString(), eq(idPersona))).thenReturn(false);
        when(personaRepositorio.existsByEmailIgnoreCaseAndIdNot(anyString(), eq(idPersona))).thenReturn(false);
        when(personaRepositorio.existsByPhoneAndIdNot(anyString(), eq(idPersona))).thenReturn(false);
        when(personaRepositorio.save(any(Persona.class))).thenReturn(persona);

        PersonaResponseDto dto = servicio.actualizarPersona(idPersona, requestValido());

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("PERSONA"), any(Persona.class));
    }

    @Test
    @DisplayName("actualizarPersona lanza RecursoNoEncontrado si no existe")
    void actualizarPersonaNoExistente() {
        when(personaRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizarPersona(idPersona, requestValido()));
    }

    @Test
    @DisplayName("actualizarPersona lanza ReglaNegocio si dni colisiona con otro")
    void actualizarPersonaDniDuplicado() {
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));
        when(personaRepositorio.existsByDniAndIdNot(anyString(), eq(idPersona))).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarPersona(idPersona, requestValido()));
        verify(personaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("actualizarPersona lanza ReglaNegocio si email colisiona con otro")
    void actualizarPersonaEmailDuplicado() {
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));
        when(personaRepositorio.existsByDniAndIdNot(anyString(), eq(idPersona))).thenReturn(false);
        when(personaRepositorio.existsByEmailIgnoreCaseAndIdNot(anyString(), eq(idPersona))).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarPersona(idPersona, requestValido()));
        verify(personaRepositorio, never()).save(any());
    }

    @Test
    @DisplayName("actualizarPersona lanza ReglaNegocio si telefono colisiona con otro")
    void actualizarPersonaTelefonoDuplicado() {
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));
        when(personaRepositorio.existsByDniAndIdNot(anyString(), eq(idPersona))).thenReturn(false);
        when(personaRepositorio.existsByEmailIgnoreCaseAndIdNot(anyString(), eq(idPersona))).thenReturn(false);
        when(personaRepositorio.existsByPhoneAndIdNot(anyString(), eq(idPersona))).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarPersona(idPersona, requestValido()));
        verify(personaRepositorio, never()).save(any());
    }

    // ---- activarPersona ----

    @Test
    @DisplayName("activarPersona pone active=true y publica UPDATE")
    void activarPersonaOk() {
        persona.setActive(false);
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));
        when(personaRepositorio.save(any(Persona.class))).thenReturn(persona);

        PersonaResponseDto dto = servicio.activarPersona(idPersona);

        assertNotNull(dto);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("PERSONA"), any(Persona.class));
    }

    @Test
    @DisplayName("activarPersona lanza RecursoNoEncontrado si no existe")
    void activarPersonaNoExistente() {
        when(personaRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.activarPersona(idPersona));
    }

    // ---- desactivarPersona ----

    @Test
    @DisplayName("desactivarPersona sin usuario asociado solo desactiva persona")
    void desactivarPersonaSinUsuario() {
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));
        when(usuarioRepositorio.existsById(idPersona)).thenReturn(false);
        when(personaRepositorio.save(any(Persona.class))).thenReturn(persona);

        PersonaResponseDto dto = servicio.desactivarPersona(idPersona);

        assertNotNull(dto);
        verify(usuarioServicio, never()).desactivarUsuario(any());
        verify(auditPublisher).publicar(eq("UPDATE"), eq("PERSONA"), any(Persona.class));
    }

    @Test
    @DisplayName("desactivarPersona con usuario asociado desactiva en cascada")
    void desactivarPersonaConUsuario() {
        when(personaRepositorio.findById(idPersona)).thenReturn(Optional.of(persona));
        when(usuarioRepositorio.existsById(idPersona)).thenReturn(true);
        when(personaRepositorio.save(any(Persona.class))).thenReturn(persona);

        servicio.desactivarPersona(idPersona);

        verify(usuarioServicio).desactivarUsuario(idPersona);
        verify(auditPublisher).publicar(eq("UPDATE"), eq("PERSONA"), any(Persona.class));
    }

    @Test
    @DisplayName("desactivarPersona lanza RecursoNoEncontrado si no existe")
    void desactivarPersonaNoExistente() {
        when(personaRepositorio.findById(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.desactivarPersona(idPersona));
    }

    // ---- buscarPersonas ----

    @Test
    @DisplayName("buscarPersonas lanza IllegalArgument si no hay criterios")
    void buscarPersonasSinCriterios() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.buscarPersonas(null, null, null));
    }

    @Test
    @DisplayName("buscarPersonas lanza IllegalArgument si criterios estan vacios")
    void buscarPersonasCriteriosVacios() {
        assertThrows(IllegalArgumentException.class,
                () -> servicio.buscarPersonas("", "", ""));
    }

    @Test
    @DisplayName("buscarPersonas por dni retorna resultado")
    void buscarPersonasPorDni() {
        when(personaRepositorio.findByDni("1713175071")).thenReturn(Optional.of(persona));

        List<PersonaResponseDto> resultado = servicio.buscarPersonas("1713175071", null, null);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("buscarPersonas por dni sin resultado retorna lista vacia")
    void buscarPersonasPorDniSinResultado() {
        when(personaRepositorio.findByDni(anyString())).thenReturn(Optional.empty());

        List<PersonaResponseDto> resultado = servicio.buscarPersonas("9999999999", null, null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("buscarPersonas por apellido retorna lista")
    void buscarPersonasPorApellido() {
        when(personaRepositorio.findByLastNameContainingIgnoreCase("Perez")).thenReturn(List.of(persona));

        List<PersonaResponseDto> resultado = servicio.buscarPersonas(null, null, "Perez");

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("buscarPersonas por nombre cuando no hay dni ni apellido")
    void buscarPersonasPorNombre() {
        when(personaRepositorio.findByFirstNameContainingIgnoreCase("Juan")).thenReturn(List.of(persona));

        List<PersonaResponseDto> resultado = servicio.buscarPersonas(null, "Juan", null);

        assertEquals(1, resultado.size());
    }
}
