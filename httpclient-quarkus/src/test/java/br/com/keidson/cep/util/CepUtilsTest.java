package br.com.keidson.cep.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.keidson.cep.exception.CepInvalidoException;

class CepUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = { "69005040", "69005-040", "69.005-040", " 69005 040 " })
    void deveNormalizarFormatosAceitos(String entrada) {
        assertEquals("69005040", CepUtils.normalizar(entrada));
        assertEquals("69005-040", CepUtils.formatar(entrada));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "123", "690050400", "abcde-fgh", "69005-04a" })
    void deveRejeitarCepInvalido(String entrada) {
        assertThrows(CepInvalidoException.class, () -> CepUtils.normalizar(entrada));
    }
}
