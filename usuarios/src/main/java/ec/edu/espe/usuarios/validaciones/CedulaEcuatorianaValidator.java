package ec.edu.espe.usuarios.validaciones;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CedulaEcuatorianaValidator implements ConstraintValidator<CedulaEcuatoriana, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String cedula = value.trim();
        if (!cedula.matches("^\\d{10}$")) {
            return false;
        }

        int provincia = Integer.parseInt(cedula.substring(0, 2));
        int tercerDigito = Character.getNumericValue(cedula.charAt(2));
        if (provincia < 1 || provincia > 24) {
            return false;
        }
        if (tercerDigito < 0 || tercerDigito > 5) {
            return false;
        }

        int suma = 0;
        for (int i = 0; i < 9; i++) {
            int digito = Character.getNumericValue(cedula.charAt(i));
            int multiplicado = digito * (i % 2 == 0 ? 2 : 1);
            if (multiplicado > 9) {
                multiplicado -= 9;
            }
            suma += multiplicado;
        }

        int digitoVerificador = (10 - (suma % 10)) % 10;
        return digitoVerificador == Character.getNumericValue(cedula.charAt(9));
    }
}