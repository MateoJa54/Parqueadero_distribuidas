package ec.edu.espe.zonas.entidades;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "espacios")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String codigo;

    @Column(length = 150)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEspacio tipoEspacio;

    @Column(nullable= false)
    private boolean activo; //true: activo, false: inactivo

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEspacio estado;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="id_zona")
    private Zona zona;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaModificacion;

    @PrePersist
    protected void alCrear() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = this.fechaCreacion;
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaModificacion = LocalDateTime.now();
    }

}
