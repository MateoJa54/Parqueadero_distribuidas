package ec.edu.espe.usuarios.validaciones;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = CedulaEcuatorianaValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface CedulaEcuatoriana {

    String message() default "La cedula ecuatoriana no es valida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}