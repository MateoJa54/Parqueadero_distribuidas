package ec.edu.espe.zonas.controlller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.zonas.dtos.DisponibilidadResponseDto;
import ec.edu.espe.zonas.dtos.EspacioRequestDto;
import ec.edu.espe.zonas.dtos.EspacioRespondeDto;
import ec.edu.espe.zonas.entidades.EstadoEspacio;
import ec.edu.espe.zonas.entidades.TipoEspacio;
import ec.edu.espe.zonas.services.EspacioServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/espacios")
@RequiredArgsConstructor
public class EspacioController {

    private final EspacioServicio espacioServicio;

    @GetMapping
    public ResponseEntity<List<EspacioRespondeDto>> listarEspacios() {
        return ResponseEntity.ok(espacioServicio.obtenerEspacio());
    }

    @GetMapping("/{idEspacio}")
    public ResponseEntity<EspacioRespondeDto> obtenerEspacio(@PathVariable UUID idEspacio) {
        return ResponseEntity.ok(espacioServicio.obtenerEspacioPorId(idEspacio));
    }

    @PostMapping
    public ResponseEntity<EspacioRespondeDto> crearEspacio(@Valid @RequestBody EspacioRequestDto request) {
        return new ResponseEntity<>(espacioServicio.crearEspacio(request), HttpStatus.CREATED);
    }

    @PutMapping("/{idEspacio}")
    public ResponseEntity<EspacioRespondeDto> actualizarEspacio(
            @PathVariable UUID idEspacio,
            @Valid @RequestBody EspacioRequestDto request) {
        return ResponseEntity.ok(espacioServicio.actualizarEspacio(idEspacio, request));
    }

    @PatchMapping("/{idEspacio}/estado")
    public ResponseEntity<EspacioRespondeDto> cambiarEstado(
            @PathVariable UUID idEspacio,
            @RequestParam EstadoEspacio estado) {
        return ResponseEntity.ok(espacioServicio.cambiarEstado(idEspacio, estado));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<EspacioRespondeDto>> listarPorEstado(@PathVariable EstadoEspacio estado) {
        return ResponseEntity.ok(espacioServicio.obtnerEspacioPOrEstado(estado));
    }

    // Lista de espacios DISPONIBLES, filtrable por zona y/o tipo.
    // Ej: /api/v1/espacios/disponibles?idZona=...&tipo=AUTO
    @GetMapping("/disponibles")
    public ResponseEntity<List<EspacioRespondeDto>> listarDisponibles(
            @RequestParam(required = false) UUID idZona,
            @RequestParam(required = false) TipoEspacio tipo) {
        return ResponseEntity.ok(espacioServicio.listarDisponibles(idZona, tipo));
    }

    // Verifica si un espacio puntual puede usarse ahora (activo y DISPONIBLE).
    @GetMapping("/{idEspacio}/disponibilidad")
    public ResponseEntity<DisponibilidadResponseDto> verificarDisponibilidad(@PathVariable UUID idEspacio) {
        return ResponseEntity.ok(espacioServicio.verificarDisponibilidad(idEspacio));
    }

    @GetMapping("/zona/{idZona}/estado/{estado}")
    public ResponseEntity<EspacioRespondeDto> buscarPorZonaYEstado(
            @PathVariable UUID idZona,
            @PathVariable EstadoEspacio estado) {
        return ResponseEntity.ok(espacioServicio.obtenerEspacioPorZonaEstado(idZona, estado));
    }

    @PatchMapping("/{idEspacio}/activar")
    public ResponseEntity<Void> activarEspacio(@PathVariable UUID idEspacio) {
        espacioServicio.activarEspacio(idEspacio);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idEspacio}/desactivar")
    public ResponseEntity<Void> desactivarEspacio(@PathVariable UUID idEspacio) {
        espacioServicio.desactivarEspacio(idEspacio);
        return ResponseEntity.noContent().build();
    }
}
