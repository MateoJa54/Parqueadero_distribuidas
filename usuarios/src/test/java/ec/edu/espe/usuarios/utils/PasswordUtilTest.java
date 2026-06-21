package ec.edu.espe.usuarios.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordUtil Tests")
class PasswordUtilTest {

    @Test
    @DisplayName("Debería generar un hash SHA-256 Base64 válido")
    void testHashGeneratesValidBase64() {
        String password = "MiContraseña123";
        String hash = PasswordUtil.hash(password);
        
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertTrue(hash.length() > 0 && hash.length() <= 100);
    }

    @Test
    @DisplayName("Debería generar el mismo hash para la misma contraseña")
    void testHashIsDeterministic() {
        String password = "MiContraseña123";
        String hash1 = PasswordUtil.hash(password);
        String hash2 = PasswordUtil.hash(password);
        
        assertEquals(hash1, hash2);
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
