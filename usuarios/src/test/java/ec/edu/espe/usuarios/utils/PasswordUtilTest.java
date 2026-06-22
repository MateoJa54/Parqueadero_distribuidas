package ec.edu.espe.usuarios.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordUtil Tests")
class PasswordUtilTest {

    @Test
    @DisplayName("Deberia generar un hash BCrypt valido")
    void testHashGeneratesValidBase64() {
        String password = "MiContraseña123";
        String hash = PasswordUtil.hash(password);
        
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertTrue(hash.length() > 0 && hash.length() <= 100);
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
    @DisplayName("Debería manejar contraseñas largas")
    void testHashWithLongPassword() {
        String password = "A".repeat(100);
        String hash = PasswordUtil.hash(password);
        
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertTrue(hash.length() <= 100);
    }
}
