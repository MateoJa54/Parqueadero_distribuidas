package ec.edu.espe.zonas.services.Impl;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import ec.edu.espe.zonas.ZonasApplication;
import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.dtos.ZonaRespondeDto;
import ec.edu.espe.zonas.dtos.ZonaRequestDto;
import ec.edu.espe.zonas.entidades.Espacio;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import ec.edu.espe.zonas.entidades.TipoZona;
import ec.edu.espe.zonas.entidades.Zona;
import ec.edu.espe.zonas.repositorios.EspacioRepositorio;
import ec.edu.espe.zonas.repositorios.ZonaRepositorio;

public class ZonaCicloVidaMain {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ZonasApplication.class, args);
        try {
            ZonaServicioImpl zonaServicio = context.getBean(ZonaServicioImpl.class);
            EspacioServicioImpl espacioServicio = context.getBean(EspacioServicioImpl.class);
            ZonaRepositorio zonaRepositorio = context.getBean(ZonaRepositorio.class);
            EspacioRepositorio espacioRepositorio = context.getBean(EspacioRepositorio.class);

            long sufijo = System.currentTimeMillis();

            // 1) Zona con capacidad = 2
            ZonaRespondeDto zona = zonaServicio.crearZona(ZonaRequestDto.builder()
                    .nombre("Zona Ciclo " + sufijo)
                    .descripcion("Demo ciclo de vida")
                    .tipo(TipoZona.REGULAR)
                    .capacidad(2)
                    .build());
            UUID idZona = zona.getIdZona();
            System.out.println("Zona creada: " + zona.getCodigo() + " capacidad=" + zona.getCapacidad());

            // 2) Creamos 2 espacios (OK, dentro de la capacidad)
            crearEspacio(espacioServicio, idZona, "ESP-A-" + (sufijo % 1000));
            crearEspacio(espacioServicio, idZona, "ESP-B-" + (sufijo % 1000));

            // 3) Tercer espacio -> debe FALLAR por capacidad
            System.out.println("\n--- Intentando 3er espacio (debe fallar) ---");
            crearEspacio(espacioServicio, idZona, "ESP-C-" + (sufijo % 1000));

            imprimirEspacios(zonaRepositorio, espacioRepositorio, idZona, "Estado tras crear espacios");

            // 4) Ocupamos un espacio y tratamos de desactivar la zona -> FALLA
            List<Espacio> espacios = espaciosDeZona(zonaRepositorio, espacioRepositorio, idZona);
            UUID idPrimero = espacios.get(0).getId();
            espacioServicio.cambiarEstado(idPrimero, EstadoEspacio.OCUPADO);
            System.out.println("\nEspacio " + espacios.get(0).getCodigo() + " -> OCUPADO");

            System.out.println("\n--- Desactivar zona con espacio OCUPADO (debe fallar) ---");
            try {
                zonaServicio.desactivarZona(idZona);
                System.out.println("OK -> desactivada (no esperado)");
            } catch (RuntimeException e) {
                System.out.println("FALLA-> " + e.getMessage());
            }

            // 5) Liberamos el espacio y desactivamos la zona -> OK (cascada)
            espacioServicio.cambiarEstado(idPrimero, EstadoEspacio.DISPONIBLE);
            System.out.println("\nEspacio liberado -> DISPONIBLE");
            zonaServicio.desactivarZona(idZona);
            System.out.println("Zona DESACTIVADA");
            imprimirEspacios(zonaRepositorio, espacioRepositorio, idZona,
                    "Tras desactivar (espacios -> MANTENIMIENTO / inactivo)");

            // 6) Activamos la zona -> espacios vuelven a DISPONIBLE / activo
            zonaServicio.activarZona(idZona);
            System.out.println("\nZona ACTIVADA");
            imprimirEspacios(zonaRepositorio, espacioRepositorio, idZona,
                    "Tras activar (espacios -> DISPONIBLE / activo)");

        } finally {
            context.close();
        }
    }

    private static void crearEspacio(EspacioServicioImpl servicio, UUID idZona, String codigo) {
        try {
            EspacioRespondeDto creado = servicio.crearEspacio(EspacioRequestDto.builder()
                    .idZona(idZona)
                    .descripcion("demo")
                    .tipo(TipoEspacio.AUTO)
                    .build());
            System.out.println("OK -> espacio creado: " + creado.getCodigo());
        } catch (RuntimeException e) {
            System.out.println("FALLA-> " + e.getMessage());
        }
    }

    private static List<Espacio> espaciosDeZona(ZonaRepositorio zonaRepositorio,
            EspacioRepositorio espacioRepositorio, UUID idZona) {
        Zona zona = zonaRepositorio.findById(idZona).orElseThrow();
        return espacioRepositorio.findByZona(zona);
    }

    private static void imprimirEspacios(ZonaRepositorio zonaRepositorio,
            EspacioRepositorio espacioRepositorio, UUID idZona, String titulo) {
        Zona zona = zonaRepositorio.findById(idZona).orElseThrow();
        System.out.println("\n===== " + titulo + " (zona.activo=" + zona.isActivo() + ") =====");
        for (Espacio e : espacioRepositorio.findByZona(zona)) {
            System.out.println("  " + e.getCodigo()
                    + " | estado=" + e.getEstado()
                    + " | activo=" + e.isActivo());
        }
    }
}
