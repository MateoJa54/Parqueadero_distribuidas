package ec.edu.espe.usuarios.validaciones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CedulaEcuatorianaValidatorTest {

    private final CedulaEcuatorianaValidator validator = new CedulaEcuatorianaValidator();

    @Test
    void debeAceptarCedulaEcuatorianaValida() {
        assertTrue(validator.isValid("1710034065", null));
    }

    @Test
    void debeRechazarCedulaConLetras() {
        assertFalse(validator.isValid("17100340A5", null));
    }

    @Test
    void debeRechazarCedulaConProvinciaInvalida() {
        assertFalse(validator.isValid("9910034065", null));
    }

    @Test
    void debeRechazarCedulaConDigitoVerificadorIncorrecto() {
        assertFalse(validator.isValid("1710034066", null));
    }
}