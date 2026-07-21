package ec.edu.espe.usuarios.entidades;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Cubre builders, getters/setters, equals/hashCode/toString y callbacks JPA de las entidades. */
class EntidadesTest {

    @Test
    void persona_builderGettersSettersYCallbacks() {
        UUID id = UUID.randomUUID();
        Persona p = Persona.builder()
                .id(id)
                .firstName("Root")
                .middleName("De")
                .lastName("Sistema")
                .dni("0000000000")
                .email("root@parqueadero.local")
                .phone("0000000000")
                .address("N/A")
                .nationality("EC")
                .active(true)
                .build();

        assertEquals(id, p.getId());
        assertEquals("Root", p.getFirstName());
        assertEquals("De", p.getMiddleName());
        assertEquals("Sistema", p.getLastName());
        assertEquals("0000000000", p.getDni());
        assertEquals("root@parqueadero.local", p.getEmail());
        assertEquals("0000000000", p.getPhone());
        assertEquals("N/A", p.getAddress());
        assertEquals("EC", p.getNationality());
        assertTrue(p.isActive());

        Persona vacio = new Persona();
        vacio.setFirstName("A");
        vacio.setLastName("B");
        assertEquals("A", vacio.getFirstName());

        Persona igual = Persona.builder().id(id).firstName("Root").lastName("Sistema")
                .dni("0000000000").email("root@parqueadero.local").phone("0000000000")
                .address("N/A").nationality("EC").middleName("De").active(true).build();
        assertEquals(p.hashCode(), igual.hashCode());
        assertEquals(p, igual);
        assertNotNull(p.toString());

        p.onCreate();
        assertNotNull(p.getCreatedAt());
        assertNotNull(p.getUpdatedAt());
        LocalDateTime creado = p.getCreatedAt();
        p.onUpdate();
        assertNotNull(p.getUpdatedAt());
        assertEquals(creado, p.getCreatedAt());
    }

    @Test
    void rol_builderGettersSettersYCallbacks() {
        UUID id = UUID.randomUUID();
        Rol r = Rol.builder().id(id).name("ADMIN").description("desc").active(true).build();

        assertEquals(id, r.getId());
        assertEquals("ADMIN", r.getName());
        assertEquals("desc", r.getDescription());
        assertTrue(r.isActive());

        Rol vacio = new Rol();
        vacio.setName("CLIENTE");
        assertEquals("CLIENTE", vacio.getName());

        Rol igual = Rol.builder().id(id).name("ADMIN").description("desc").active(true).build();
        assertEquals(r, igual);
        assertEquals(r.hashCode(), igual.hashCode());
        assertNotNull(r.toString());

        r.onCreate();
        assertNotNull(r.getCreatedAt());
        assertNotNull(r.getUpdatedAt());
        r.onUpdate();
        assertNotNull(r.getUpdatedAt());
    }

    @Test
    void usuario_builderGettersSettersYCallbacks() {
        UUID id = UUID.randomUUID();
        Persona persona = Persona.builder().id(id).firstName("Root").lastName("Sistema").build();
        Usuario u = Usuario.builder()
                .id(id)
                .persona(persona)
                .username("root")
                .passwordHash("hash")
                .lastLogin(LocalDateTime.now())
                .active(true)
                .build();

        assertEquals(id, u.getId());
        assertEquals(persona, u.getPersona());
        assertEquals("root", u.getUsername());
        assertEquals("hash", u.getPasswordHash());
        assertNotNull(u.getLastLogin());
        assertTrue(u.isActive());

        Usuario vacio = new Usuario();
        vacio.setUsername("otro");
        assertEquals("otro", vacio.getUsername());
        assertNotNull(u.toString());

        Usuario igual = new Usuario();
        igual.setId(id);
        igual.setUsername("root");
        igual.setPasswordHash("hash");
        igual.setActive(true);
        igual.setLastLogin(u.getLastLogin());
        assertEquals(u.hashCode(), igual.hashCode());
        assertEquals(u, igual);

        u.onCreate();
        assertNotNull(u.getCreatedAt());
        assertNotNull(u.getUpdatedAt());
        u.onUpdate();
        assertNotNull(u.getUpdatedAt());
    }

    @Test
    void usuarioRolId_builderGettersSettersEquals() {
        UUID user = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        UsuarioRolId pk = UsuarioRolId.builder().idUser(user).idRole(role).build();

        assertEquals(user, pk.getIdUser());
        assertEquals(role, pk.getIdRole());

        UsuarioRolId vacio = new UsuarioRolId();
        vacio.setIdUser(user);
        vacio.setIdRole(role);
        assertEquals(pk, vacio);
        assertEquals(pk.hashCode(), vacio.hashCode());
        assertNotNull(pk.toString());

        UsuarioRolId conArgs = new UsuarioRolId(user, role);
        assertEquals(pk, conArgs);
    }

    @Test
    void usuarioRol_builderGettersSettersYCallbacks() {
        UUID user = UUID.randomUUID();
        UUID role = UUID.randomUUID();
        UsuarioRolId pk = new UsuarioRolId(user, role);
        Usuario usuario = Usuario.builder().id(user).username("root").build();
        Rol rol = Rol.builder().id(role).name("ROOT").build();

        UsuarioRol ur = UsuarioRol.builder()
                .id(pk)
                .usuario(usuario)
                .rol(rol)
                .active(true)
                .build();

        assertEquals(pk, ur.getId());
        assertEquals(usuario, ur.getUsuario());
        assertEquals(rol, ur.getRol());
        assertTrue(ur.isActive());

        UsuarioRol vacio = new UsuarioRol();
        vacio.setActive(false);
        assertFalse(vacio.isActive());
        assertNotNull(ur.toString());

        UsuarioRol igual = UsuarioRol.builder().id(pk).active(true).build();
        assertEquals(ur, igual);
        assertEquals(ur.hashCode(), igual.hashCode());

        ur.onCreate();
        assertNotNull(ur.getAssignedAt());
        assertNotNull(ur.getUpdatedAt());
        ur.onUpdate();
        assertNotNull(ur.getUpdatedAt());
    }
}
