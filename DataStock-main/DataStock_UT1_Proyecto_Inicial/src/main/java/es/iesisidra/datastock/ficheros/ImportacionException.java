package es.iesisidra.datastock.ficheros;

import java.io.Serial;

/**
 * Error que afecta a TODO el documento (no a un solo producto): XML mal
 * formado, raíz incorrecta, DTD prohibido, fichero ilegible...
 *
 * <p>Cuando se lanza, la importación se CANCELA entera: no se añade ningún
 * producto y el inventario queda exactamente como estaba.</p>
 */
public class ImportacionException extends Exception {
    @Serial private static final long serialVersionUID = 1L;

    public ImportacionException(String mensaje) { super(mensaje); }

    public ImportacionException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
