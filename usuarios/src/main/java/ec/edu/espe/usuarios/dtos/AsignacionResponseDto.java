package ec.edu.espe.usuarios.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionResponseDto {

    private UUID idUser;
    private String username;
    private UUID idRole;
    private String rol;
    private boolean active;
    private LocalDateTime assignedAt;
    private LocalDateTime updatedAt;
}
