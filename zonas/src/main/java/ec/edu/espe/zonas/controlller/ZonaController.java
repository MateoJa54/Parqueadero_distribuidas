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
import org.springframework.web.bind.annotation.RestController;

import ec.edu.espe.zonas.dtos.ZonaRequestDto;
import ec.edu.espe.zonas.dtos.ZonaRespondeDto;
import ec.edu.espe.zonas.services.ZonaServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/zonas")
@RequiredArgsConstructor
public class ZonaController {

    private final ZonaServicio zonaServicio;

    @GetMapping
    public ResponseEntity<List<ZonaRespondeDto>> listarZonas() {
        return ResponseEntity.ok(zonaServicio.listarZonas());
    }

    @GetMapping("/{idZona}")
    public ResponseEntity<ZonaRespondeDto> obtenerZona(@PathVariable UUID idZona) {
        return ResponseEntity.ok(zonaServicio.obtenerZona(idZona));
    }

    @PostMapping
    public ResponseEntity<ZonaRespondeDto> crearZona(@Valid @RequestBody ZonaRequestDto request) {
        return new ResponseEntity<>(zonaServicio.crearZona(request), HttpStatus.CREATED);
    }

    @PutMapping("/{idZona}")
    public ResponseEntity<ZonaRespondeDto> actualizarZona(
            @PathVariable UUID idZona,
            @Valid @RequestBody ZonaRequestDto request) {
        return ResponseEntity.ok(zonaServicio.actualizarZona(idZona, request));
    }

    @PatchMapping("/{idZona}/activar")
    public ResponseEntity<Void> activarZona(@PathVariable UUID idZona) {
        zonaServicio.activarZona(idZona);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idZona}/desactivar")
    public ResponseEntity<Void> desactivarZona(@PathVariable UUID idZona) {
        zonaServicio.desactivarZona(idZona);
        return ResponseEntity.noContent().build();
    }
}
