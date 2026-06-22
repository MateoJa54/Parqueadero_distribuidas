package ec.edu.espe.zonas.entidades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "zonas")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 32)
    private String nombre;

    @Column(unique = true, nullable = false, length = 16)
    private String codigo;

    @Column
    private String descripcion;

    @Column
    private int capacidad;

    // Soft-delete: los espacios nunca se borran fisicamente con la zona.
    // Por eso solo se cascadea persist/merge y NO se usa orphanRemoval.
    @OneToMany(mappedBy = "zona", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Builder.Default
    private List<Espacio> espacios = new ArrayList<>();

    @Column(nullable = false)
    private boolean activo; // true: activa, false: inactiva

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoZona tipoZona;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void alCrear() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }

}