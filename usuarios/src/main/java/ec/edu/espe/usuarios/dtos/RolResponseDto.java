package ec.edu.espe.usuarios.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import ec.edu.espe.usuarios.entidades.NombreRol;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolResponseDto {

    private UUID id;
    private NombreRol name;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
