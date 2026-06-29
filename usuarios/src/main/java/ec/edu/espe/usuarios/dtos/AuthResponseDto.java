package ec.edu.espe.usuarios.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {

    private String token;
    private String tokenType;
    private UUID userId;
    private UUID idPersona;
    private String username;
    private List<String> roles;
    private boolean active;
    private LocalDateTime expiresAt;
}
