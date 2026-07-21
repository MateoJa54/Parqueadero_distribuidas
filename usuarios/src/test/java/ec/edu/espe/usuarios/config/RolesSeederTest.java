package ec.edu.espe.usuarios.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import ec.edu.espe.usuarios.entidades.Rol;
import ec.edu.espe.usuarios.repositorios.RolRepositorio;
import ec.edu.espe.usuarios.security.RolesBase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RolesSeederTest {

    @Test
    void run_ningunRolExiste_siembraTodos() {
        RolRepositorio rRepo = mock(RolRepositorio.class);
        when(rRepo.existsByName(anyString())).thenReturn(false);

        RolesSeeder seeder = new RolesSeeder(rRepo);
        seeder.run(mock(ApplicationArguments.class));

        verify(rRepo, times(RolesBase.BASE.size())).save(any(Rol.class));
    }

    @Test
    void run_todosLosRolesExisten_noSiembra() {
        RolRepositorio rRepo = mock(RolRepositorio.class);
        when(rRepo.existsByName(anyString())).thenReturn(true);

        RolesSeeder seeder = new RolesSeeder(rRepo);
        seeder.run(mock(ApplicationArguments.class));

        verify(rRepo, never()).save(any(Rol.class));
    }
}
