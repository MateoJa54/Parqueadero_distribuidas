package ec.edu.espe.zonas.dtos;

import ec.edu.espe.zonas.entidades.TipoZona;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZonaRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 1, max = 32, message = "El nombre no puede tener más de 32 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;


    @NotNull(message = "El tipo de zona es obligatorio")
    private TipoZona tipo;

    @Min(value = 1, message = "La capacidad mínima es 1")
    @Max(value = 100, message = "La capacidad máxima es 100")
    private int capacidad;    
}
