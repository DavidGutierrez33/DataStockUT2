package es.iesisidra.datastock.util;

import java.io.File;

/**
 * Pide rutas por consola y hace las comprobaciones típicas, para no repetir
 * el mismo código en cada opción del menú.
 *
 * <p>Los métodos devuelven null cuando algo va mal (ya habrán mostrado el
 * motivo): quien los llama solo tiene que comprobar "if (ruta == null)".</p>
 */
public final class SelectorRutas {

    private SelectorRutas() { }

    /** Pide la ruta de un fichero que debe existir y poder leerse (para IMPORTAR/RECUPERAR). */
    public static File pedirFicheroExistente(String mensaje) {
        String texto = LectorConsola.leerTexto(mensaje).trim();
        if (texto.isEmpty()) {
            error("No has indicado ninguna ruta.");
            return null;
        }
        File fichero = new File(texto);
        if (!fichero.exists()) {
            error("La ruta no existe: " + texto);
            return null;
        }
        if (!fichero.isFile()) {
            error("La ruta no es un fichero (¿es un directorio?): " + texto);
            return null;
        }
        if (!fichero.canRead()) {
            error("No hay permisos de lectura sobre: " + texto);
            return null;
        }
        return fichero;
    }

    /** Pide una ruta de DESTINO y, si ya existe, confirma la sobrescritura (para EXPORTAR/GUARDAR). */
    public static File pedirRutaDestino(String mensaje) {
        String texto = LectorConsola.leerTexto(mensaje).trim();
        if (texto.isEmpty()) {
            error("No has indicado ninguna ruta.");
            return null;
        }
        File destino = new File(texto);
        return destinoUtilizable(destino) ? destino : null;
    }

    /**
     * Comprueba que se puede escribir en ese destino: el directorio existe,
     * hay permisos y, si el fichero ya existe, el usuario confirma sobrescribirlo.
     *
     * @return true si se puede continuar
     */
    public static boolean destinoUtilizable(File destino) {
        if (destino.isDirectory()) {
            error("La ruta indicada es un directorio, no un fichero.");
            return false;
        }
        // getAbsoluteFile() permite conocer la carpeta padre aunque la ruta sea relativa.
        File carpeta = destino.getAbsoluteFile().getParentFile();
        if (carpeta == null || !carpeta.isDirectory()) {
            error("El directorio de destino no existe.");
            return false;
        }
        if (!carpeta.canWrite() || (destino.exists() && !destino.canWrite())) {
            error("No hay permisos de escritura en el destino.");
            return false;
        }
        if (destino.exists() && !LectorConsola.confirmar("El fichero ya existe. ¿Sobrescribir?")) {
            System.out.println(ColoresConsola.pintar("Operación cancelada. El fichero original no se ha tocado.",
                    ColoresConsola.AMARILLO));
            return false;
        }
        return true;
    }

    private static void error(String mensaje) {
        System.out.println(ColoresConsola.pintar("ERROR: " + mensaje, ColoresConsola.ROJO));
    }
}
