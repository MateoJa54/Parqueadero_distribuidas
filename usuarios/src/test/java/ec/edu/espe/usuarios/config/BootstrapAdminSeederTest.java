package ec.edu.espe.usuarios.config;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import ec.edu.espe.usuarios.entidades.Persona;
import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.entidades.Usuario;
import ec.edu.espe.usuarios.entidades.UsuarioRol;
import ec.edu.espe.usuarios.repositorios.PersonaRepositorio;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRepositorio;
import ec.edu.espe.usuarios.repositorios.UsuarioRolRepositorio;
import ec.edu.espe.usuarios.security.RolesBase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BootstrapAdminSeederTest {

    private BootstrapAdminSeeder buildSeeder(PersonaRepositorio pRepo, UsuarioRepositorio uRepo,
            RolRepositorio rRepo, UsuarioRolRepositorio urRepo, String user, String pass) {
        BootstrapAdminSeeder seeder = new BootstrapAdminSeeder(pRepo, uRepo, rRepo, urRepo);
        ReflectionTestUtils.setField(seeder, "rootUsername", user);
        ReflectionTestUtils.setField(seeder, "rootPassword", pass);
        return seeder;
    }

    @Test
    void run_usuarioYaExiste_noSiembra() {
        PersonaRepositorio pRepo = mock(PersonaRepositorio.class);
        UsuarioRepositorio uRepo = mock(UsuarioRepositorio.class);
        RolRepositorio rRepo = mock(RolRepositorio.class);
        UsuarioRolRepositorio urRepo = mock(UsuarioRolRepositorio.class);
        when(uRepo.existsByUsernameIgnoreCase("root")).thenReturn(true);

        BootstrapAdminSeeder seeder = buildSeeder(pRepo, uRepo, rRepo, urRepo, "root", "secreto");
        seeder.run(mock(ApplicationArguments.class));

        verify(pRepo, never()).save(any());
        verify(uRepo, never()).save(any());
    }

    @Test
    void run_passwordVacia_noSiembra() {
        PersonaRepositorio pRepo = mock(PersonaRepositorio.class);
        UsuarioRepositorio uRepo = mock(UsuarioRepositorio.class);
        RolRepositorio rRepo = mock(RolRepositorio.class);
        UsuarioRolRepositorio urRepo = mock(UsuarioRolRepositorio.class);
        when(uRepo.existsByUsernameIgnoreCase("root")).thenReturn(false);

        BootstrapAdminSeeder seeder = buildSeeder(pRepo, uRepo, rRepo, urRepo, "root", "  ");
        seeder.run(mock(ApplicationArguments.class));

        verify(pRepo, never()).save(any());
    }

    @Test
    void run_passwordNull_noSiembra() {
        PersonaRepositorio pRepo = mock(PersonaRepositorio.class);
        UsuarioRepositorio uRepo = mock(UsuarioRepositorio.class);
        RolRepositorio rRepo = mock(RolRepositorio.class);
        UsuarioRolRepositorio urRepo = mock(UsuarioRolRepositorio.class);
        when(uRepo.existsByUsernameIgnoreCase("root")).thenReturn(false);

        BootstrapAdminSeeder seeder = buildSeeder(pRepo, uRepo, rRepo, urRepo, "root", null);
        seeder.run(mock(ApplicationArguments.class));

        verify(pRepo, never()).save(any());
    }

    @Test
    void run_personaNueva_siembraTodo() {
        PersonaRepositorio pRepo = mock(PersonaRepositorio.class);
        UsuarioRepositorio uRepo = mock(UsuarioRepositorio.class);
        RolRepositorio rRepo = mock(RolRepositorio.class);
        UsuarioRolRepositorio urRepo = mock(UsuarioRolRepositorio.class);

        when(uRepo.existsByUsernameIgnoreCase("root")).thenReturn(false);
        when(pRepo.findByDni("0000000000")).thenReturn(Optional.empty());
        when(pRepo.save(any(Persona.class))).thenAnswer(inv -> {
            Persona p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(uRepo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        Rol rolRoot = Rol.builder().id(UUID.randomUUID()).name(RolesBase.ROOT).build();
        when(rRepo.findByName(RolesBase.ROOT)).thenReturn(Optional.of(rolRoot));

        BootstrapAdminSeeder seeder = buildSeeder(pRepo, uRepo, rRepo, urRepo, "root", "secreto");
        seeder.run(mock(ApplicationArguments.class));

        verify(pRepo).save(any(Persona.class));
        verify(uRepo).save(any(Usuario.class));
        verify(urRepo).save(any(UsuarioRol.class));
    }

    @Test
    void run_personaExistente_reusaYSiembraUsuario() {
        PersonaRepositorio pRepo = mock(PersonaRepositorio.class);
        UsuarioRepositorio uRepo = mock(UsuarioRepositorio.class);
        RolRepositorio rRepo = mock(RolRepositorio.class);
        UsuarioRolRepositorio urRepo = mock(UsuarioRolRepositorio.class);

        when(uRepo.existsByUsernameIgnoreCase("root")).thenReturn(false);
        Persona existente = Persona.builder().id(UUID.randomUUID()).dni("0000000000").build();
        when(pRepo.findByDni("0000000000")).thenReturn(Optional.of(existente));
        when(uRepo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(rRepo.findByName(RolesBase.ROOT))
                .thenReturn(Optional.of(Rol.builder().id(UUID.randomUUID()).name(RolesBase.ROOT).build()));

        BootstrapAdminSeeder seeder = buildSeeder(pRepo, uRepo, rRepo, urRepo, "root", "secreto");
        seeder.run(mock(ApplicationArguments.class));

        verify(pRepo, never()).save(any(Persona.class));
        verify(uRepo).save(any(Usuario.class));
        verify(urRepo).save(any(UsuarioRol.class));
    }

    @Test
    void run_rolRootNoSembrado_lanzaExcepcion() {
        PersonaRepositorio pRepo = mock(PersonaRepositorio.class);
        UsuarioRepositorio uRepo = mock(UsuarioRepositorio.class);
        RolRepositorio rRepo = mock(RolRepositorio.class);
        UsuarioRolRepositorio urRepo = mock(UsuarioRolRepositorio.class);

        when(uRepo.existsByUsernameIgnoreCase("root")).thenReturn(false);
        when(pRepo.findByDni("0000000000"))
                .thenReturn(Optional.of(Persona.builder().id(UUID.randomUUID()).build()));
        when(uRepo.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(rRepo.findByName(RolesBase.ROOT)).thenReturn(Optional.empty());

        BootstrapAdminSeeder seeder = buildSeeder(pRepo, uRepo, rRepo, urRepo, "root", "secreto");
        assertThrows(IllegalStateException.class, () -> seeder.run(mock(ApplicationArguments.class)));
    }
}
