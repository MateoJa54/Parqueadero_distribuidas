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
public class PersonaResponseDto {

    private UUID id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String dni;
    private String email;
    private String phone;
    private String address;
    private String nationality;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
