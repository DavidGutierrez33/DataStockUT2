package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RF-FIC-08: fichero binario de registros de LONGITUD FIJA con RandomAccessFile.
 *
 * <p>Cada registro ocupa siempre 36 bytes:</p>
 * <pre>
 *  bytes  0..23  código   12 caracteres x 2 bytes (writeChar guarda 2 bytes)
 *  bytes 24..31  precio   double (8 bytes)
 *  bytes 32..35  stock    int    (4 bytes)
 * </pre>
 *
 * <p>Como todos los registros miden lo mismo, el registro número N empieza en
 * el byte N x 36. Con seek() saltamos directamente ahí SIN leer los anteriores:
 * eso es el acceso aleatorio.</p>
 *
 * <p>stock.bin es una copia INDEPENDIENTE: cambiarlo no modifica los
 * productos en memoria, ni al revés.</p>
 */
public final class StockBinario {

    public static final int LONGITUD_CODIGO = 12;
    public static final int TAMANO_REGISTRO = 36;
    /** Dentro de un registro, el stock empieza tras 24 bytes de código + 8 de precio. */
    private static final int DESPLAZAMIENTO_STOCK = 32;

    /** Datos de un registro leído (record = clase inmutable muy compacta). */
    public record RegistroStock(String codigo, double precio, int stock) { }

    private StockBinario() { }

    // ------------------------------------------------------------------
    // Generar el fichero
    // ------------------------------------------------------------------

    /**
     * Crea (o recrea) el fichero con una copia de los productos, ordenada por código.
     *
     * @return número de registros escritos
     * @throws IOException si falla la escritura
     */
    public static int generar(List<Producto> productos, File fichero) throws IOException {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort(Comparator.comparing(Producto::getCodigo, String.CASE_INSENSITIVE_ORDER));

        // "rw" = lectura y escritura; si no existe, se crea.
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "rw")) {
            raf.setLength(0); // TRUNCAR: borra el contenido anterior antes de escribir

            for (Producto p : copia) {
                escribirCodigo(raf, p.getCodigo());
                raf.writeDouble(p.getPrecio().doubleValue()); // el modelo usa BigDecimal; aquí double
                raf.writeInt(p.getStock());
            }
        }
        return copia.size();
    }

    /** Escribe EXACTAMENTE 12 caracteres: rellena con espacios por la derecha. */
    private static void escribirCodigo(RandomAccessFile raf, String codigo) throws IOException {
        for (int i = 0; i < LONGITUD_CODIGO; i++) {
            char c = (i < codigo.length()) ? codigo.charAt(i) : ' ';
            raf.writeChar(c);
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Cuenta los registros comprobando que el fichero es válido.
     *
     * @throws IOException si no existe o su tamaño no es múltiplo de 36
     */
    public static int contarRegistros(File fichero) throws IOException {
        comprobarQueExiste(fichero);
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "r")) {
            return totalRegistros(raf);
        }
    }

    /**
     * Lee UN registro saltando directamente a él con seek().
     *
     * @param posicion posición lógica, empezando en 0
     * @throws IllegalArgumentException si la posición está fuera de rango
     * @throws IOException si el fichero no existe o es inválido
     */
    public static RegistroStock leer(File fichero, int posicion) throws IOException {
        comprobarQueExiste(fichero);
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "r")) {
            int total = totalRegistros(raf);
            comprobarPosicion(posicion, total);

            // (long) evita desbordamientos con ficheros muy grandes.
            raf.seek((long) posicion * TAMANO_REGISTRO);
            return leerRegistroActual(raf);
        }
    }

    /** Lectura SECUENCIAL de todos los registros, del primero al último. */
    public static List<RegistroStock> leerTodos(File fichero) throws IOException {
        comprobarQueExiste(fichero);
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "r")) {
            int total = totalRegistros(raf);
            List<RegistroStock> lista = new ArrayList<>();
            raf.seek(0);
            for (int i = 0; i < total; i++) {
                lista.add(leerRegistroActual(raf)); // cada lectura avanza el puntero
            }
            return lista;
        }
    }

    // ------------------------------------------------------------------
    // Modificación
    // ------------------------------------------------------------------

    /**
     * Cambia SOLO el stock de un registro, sin reescribir el resto.
     *
     * @return el stock que había antes del cambio
     * @throws IllegalArgumentException si la posición no existe o el stock es negativo
     */
    public static int modificarStock(File fichero, int posicion, int nuevoStock) throws IOException {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
        comprobarQueExiste(fichero);
        try (RandomAccessFile raf = new RandomAccessFile(fichero, "rw")) {
            int total = totalRegistros(raf);
            comprobarPosicion(posicion, total);

            long posicionStock = (long) posicion * TAMANO_REGISTRO + DESPLAZAMIENTO_STOCK;

            raf.seek(posicionStock);
            int stockAnterior = raf.readInt();   // leer avanza el puntero 4 bytes...
            raf.seek(posicionStock);             // ...así que volvemos al mismo sitio
            raf.writeInt(nuevoStock);            // y sobrescribimos solo esos 4 bytes
            return stockAnterior;
        }
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /** Lee un registro desde donde esté el puntero del fichero. */
    private static RegistroStock leerRegistroActual(RandomAccessFile raf) throws IOException {
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < LONGITUD_CODIGO; i++) {
            codigo.append(raf.readChar());
        }
        double precio = raf.readDouble();
        int stock = raf.readInt();
        return new RegistroStock(codigo.toString().trim(), precio, stock); // trim quita el relleno
    }

    /** Un fichero válido tiene un tamaño múltiplo de 36. */
    private static int totalRegistros(RandomAccessFile raf) throws IOException {
        long longitud = raf.length();
        if (longitud % TAMANO_REGISTRO != 0) {
            throw new IOException("fichero inválido: su tamaño (" + longitud
                    + " bytes) no es múltiplo de " + TAMANO_REGISTRO);
        }
        return (int) (longitud / TAMANO_REGISTRO);
    }

    private static void comprobarPosicion(int posicion, int total) {
        if (posicion < 0 || posicion >= total) {
            throw new IllegalArgumentException(total == 0
                    ? "El fichero no tiene registros."
                    : "Posición fuera de rango: debe estar entre 0 y " + (total - 1) + ".");
        }
    }

    /** Evita que "rw" cree un fichero vacío por accidente al consultar. */
    private static void comprobarQueExiste(File fichero) throws IOException {
        if (!fichero.isFile()) {
            throw new IOException("el fichero no existe: " + fichero.getPath());
        }
    }
}
