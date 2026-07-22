package ec.edu.espe.usuarios.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordUtil Tests")
class PasswordUtilTest {

    @Test
    @DisplayName("Deberia generar un hash BCrypt valido")
    void testHashGeneratesValidBase64() {
        String password = "MiContraseña123";
        String hash = PasswordUtil.hash(password);
        
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertTrue(hash.length() <= 100);
    }

    @Test
    @DisplayName("Deberia generar hashes distintos para la misma contrasena (salt aleatorio de BCrypt)")
    void testHashIsSalted() {
        String password = "MiContraseña123";
        String hash1 = PasswordUtil.hash(password);
        String hash2 = PasswordUtil.hash(password);

        // BCrypt usa un salt aleatorio: el mismo texto produce hashes distintos,
        // pero ambos validan correctamente contra la contrasena original.
        assertNotEquals(hash1, hash2);
        assertTrue(PasswordUtil.matches(password, hash1));
        assertTrue(PasswordUtil.matches(password, hash2));
    }

    @Test
    @DisplayName("Debería generar hashes diferentes para contraseñas diferentes")
    void testDifferentPasswordsProduceDifferentHashes() {
        String hash1 = PasswordUtil.hash("Contraseña1");
        String hash2 = PasswordUtil.hash("Contraseña2");
        
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Debería manejar contraseñas con caracteres especiales")
    void testHashWithSpecialCharacters() {
        String password = "P@ssw0rd!#$%^&*()";
        String hash = PasswordUtil.hash(password);
        
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    @DisplayName("matches con contrasena incorrecta retorna false")
    void matches_wrongPassword_returnsFalse() {
        String hash = PasswordUtil.hash("correctPassword");
        assertFalse(PasswordUtil.matches("wrongpass", hash));
    }

    @Test
    @DisplayName("Debería manejar contraseñas largas")
    void testHashWithLongPassword() {
        // BCrypt admite hasta 72 bytes; la app limita la contrasena a 30 caracteres,
        // por lo que se prueba una contrasena larga pero dentro del limite del algoritmo.
        String password = "A".repeat(72);
        String hash = PasswordUtil.hash(password);
        
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertTrue(hash.length() <= 100);
    }
}
