package ec.edu.espe.usuarios.entidades;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Usuario del sistema. Comparte la clave primaria con {@link Persona}
 * (relacion 1 a 1 mediante {@code @PrimaryKeyJoinColumn}), tal como muestra el diagrama:
 * la PK de "users" es {@code id_person}.
 */
@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Usuario {

    // PK compartida con Persona (relacion 1 a 1). El id NO se autogenera:
    // @MapsId copia el id de la persona asociada como clave primaria de users.
    @Id
    @Column(name = "id_person")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id_person")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Persona persona;

    @Column(name = "username", unique = true, nullable = false, length = 15)
    private String username;

    // Nunca debe salir en un JSON: ni en la API (no aplica, se usa DTO aparte)
    // ni en los eventos de auditoria (que serializan esta entidad tal cual).
    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
