package es.iesisidra.datastock.validacion;

import java.io.Serial;

/**
 * Se lanza cuando UN producto concreto no cumple las reglas del enunciado
 * (precio negativo, código vacío, stock que no es un número...).
 *
 * <p>Es una excepción "comprobada" (hereda de Exception): Java nos obliga a
 * tratarla con try/catch. Así no podemos olvidarnos de gestionar los
 * productos incorrectos. El mensaje (getMessage) explica el motivo y es el
 * que se mostrará en la lista de incidencias.</p>
 */
public class ProductoInvalidoException extends Exception {
    @Serial private static final long serialVersionUID = 1L;

    /** @param motivo texto que explica qué regla se ha incumplido. */
    public ProductoInvalidoException(String motivo) {
        super(motivo);
    }
}
