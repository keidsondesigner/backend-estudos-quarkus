package br.com.keidson.cep.util;

import java.util.regex.Pattern;

import br.com.keidson.cep.exception.CepInvalidoException;

/**
 * Normalização e formatação de CEP.
 * Aceita "69005-040", "69005040", "69.005-040" e " 69005 040 ".
 */
public final class CepUtils {

    private static final Pattern OITO_DIGITOS = Pattern.compile("\\d{8}");
    private static final Pattern SEPARADORES = Pattern.compile("[\\s.\\-]");

    private CepUtils() {
    }

    /** Remove separadores e valida. Retorna apenas os 8 dígitos (ex.: 69005040). */
    public static String normalizar(String cep) {
        if (cep == null || cep.isBlank()) {
            throw new CepInvalidoException("CEP não informado");
        }
        String somenteDigitos = SEPARADORES.matcher(cep).replaceAll("");
        if (!OITO_DIGITOS.matcher(somenteDigitos).matches()) {
            throw new CepInvalidoException("CEP inválido: '" + cep + "'. Informe 8 dígitos, ex.: 69005-040");
        }
        return somenteDigitos;
    }

    /** Formata no padrão 00000-000 (ex.: 69005-040). */
    public static String formatar(String cep) {
        String n = normalizar(cep);
        return n.substring(0, 5) + "-" + n.substring(5);
    }
}
