package es.iesisidra.datastock.validacion;

import es.iesisidra.datastock.modelo.Producto;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas de validación de un producto (apartado 6 del enunciado).
 *
 * <p>IMPORTANTE: está escrita UNA sola vez y la usan todos:
 * la importación XML (RF-FIC-03), la importación JSON (RF-FIC-02) y la
 * recuperación del fichero serializado (RF-FIC-06). Así garantizamos que
 * JSON y XML aplican "exactamente las mismas validaciones".</p>
 *
 * <p>Es una clase de utilidad: todos sus métodos son static y no hace falta
 * crear objetos de ella (por eso el constructor es privado).</p>
 */
public final class ValidadorProducto {

    /** Patrón del modelo de datos: solo mayúsculas, dígitos y guion. */
    private static final Pattern PATRON_CODIGO = Pattern.compile("[A-Z0-9-]+");
    private static final int MAX_CODIGO = 12;
    private static final int MIN_NOMBRE = 3;
    private static final int MAX_NOMBRE = 100;
    /** Máximo que cabe en DECIMAL(10,2) de MySQL: 8 cifras enteras y 2 decimales. */
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");

    private ValidadorProducto() { }

    // ------------------------------------------------------------------
    // 1) Crear un Producto a partir de TEXTOS (lo que sale de un JSON/XML)
    // ------------------------------------------------------------------

    /**
     * Convierte los seis campos (en forma de texto) en un Producto válido.
     *
     * <p>En un fichero todo es texto: el precio llega como "19.95", el stock
     * como "12"... Aquí los convertimos al tipo Java correcto y comprobamos
     * las reglas. Si algo falla se lanza ProductoInvalidoException con el
     * motivo; se informa del PRIMER problema encontrado.</p>
     *
     * @throws ProductoInvalidoException si cualquier campo es incorrecto
     */
    public static Producto crearDesdeTextos(String codigo, String nombre, String precio,
                                            String stock, String categoriaId, String proveedorId)
            throws ProductoInvalidoException {

        // trim() elimina espacios al principio y al final. Además pasamos el
        // código a MAYÚSCULAS: el enunciado dice que "tec-001" y "TEC-001" son el
        // mismo código (único sin distinguir mayúsculas) y el patrón solo admite A-Z.
        // Así "tec-001" se guarda como TEC-001 y, si ya existe, se detecta como duplicado.
        // Si el texto es null (etiqueta ausente) lo dejamos en null y validarCodigo lo detectará.
        String codigoLimpio = (codigo == null) ? null : codigo.trim().toUpperCase(Locale.ROOT);
        validarCodigo(codigoLimpio);

        String nombreLimpio = (nombre == null) ? null : nombre.trim();
        validarNombre(nombreLimpio);

        BigDecimal precioNum = convertirDecimal(precio, "el precio");
        validarPrecio(precioNum);

        int stockNum = convertirEntero(stock, "el stock");
        validarStock(stockNum);

        int categoriaNum = convertirEntero(categoriaId, "el categoriaId");
        validarId(categoriaNum, "el categoriaId");

        int proveedorNum = convertirEntero(proveedorId, "el proveedorId");
        validarId(proveedorNum, "el proveedorId");

        return new Producto(codigoLimpio, nombreLimpio, precioNum, stockNum, categoriaNum, proveedorNum);
    }

    // ------------------------------------------------------------------
    // 2) Validar un Producto que YA es un objeto (p. ej. tras deserializar)
    // ------------------------------------------------------------------

    /**
     * Comprueba que un Producto ya construido cumple las reglas.
     * Se usa al recuperar un .dat: no nos fiamos de lo que venga en el fichero.
     */
    public static void validar(Producto p) throws ProductoInvalidoException {
        if (p == null) {
            throw new ProductoInvalidoException("el producto es nulo");
        }
        validarCodigo(p.getCodigo());
        validarNombre(p.getNombre() == null ? null : p.getNombre().trim());
        validarPrecio(p.getPrecio());
        validarStock(p.getStock());
        validarId(p.getCategoriaId(), "el categoriaId");
        validarId(p.getProveedorId(), "el proveedorId");
    }

    // ------------------------------------------------------------------
    // Reglas individuales (una por campo, para que sean fáciles de leer)
    // En la UT2 son públicas porque los menús MySQL piden los datos uno a uno
    // y validan cada campo nada más escribirlo.
    // ------------------------------------------------------------------

    /** Quita espacios y pasa a mayúsculas: "tec-001 " se convierte en "TEC-001". */
    public static String normalizarCodigo(String codigo) {
        return (codigo == null) ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    public static void validarCodigo(String codigo) throws ProductoInvalidoException {
        if (codigo == null || codigo.isEmpty()) {
            throw new ProductoInvalidoException("el código es obligatorio");
        }
        if (codigo.length() > MAX_CODIGO) {
            throw new ProductoInvalidoException("el código no puede superar " + MAX_CODIGO + " caracteres");
        }
        // matches() exige que TODO el texto cumpla el patrón, no solo una parte.
        if (!PATRON_CODIGO.matcher(codigo).matches()) {
            throw new ProductoInvalidoException("el código solo admite mayúsculas, números y guiones (A-Z, 0-9, -)");
        }
    }

    public static void validarNombre(String nombre) throws ProductoInvalidoException {
        if (nombre == null || nombre.isEmpty()) {
            throw new ProductoInvalidoException("el nombre es obligatorio");
        }
        if (nombre.length() < MIN_NOMBRE || nombre.length() > MAX_NOMBRE) {
            throw new ProductoInvalidoException("el nombre debe tener entre " + MIN_NOMBRE + " y " + MAX_NOMBRE + " caracteres");
        }
    }

    public static void validarPrecio(BigDecimal precio) throws ProductoInvalidoException {
        if (precio == null) {
            throw new ProductoInvalidoException("el precio es obligatorio");
        }
        // signum() devuelve -1 (negativo), 0 (cero) o 1 (positivo).
        // Con BigDecimal NO se compara con "<=", se usa signum() o compareTo().
        if (precio.signum() <= 0) {
            throw new ProductoInvalidoException("el precio debe ser mayor que 0");
        }
        // UT2: la columna es DECIMAL(10,2). stripTrailingZeros() hace que 49.90 cuente
        // como 49.9 (1 decimal) y así solo rechazamos los que tienen 3 o más decimales reales.
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new ProductoInvalidoException("el precio admite como máximo 2 decimales");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new ProductoInvalidoException("el precio no puede superar " + PRECIO_MAXIMO.toPlainString());
        }
    }

    public static void validarStock(int stock) throws ProductoInvalidoException {
        if (stock < 0) {
            throw new ProductoInvalidoException("el stock no puede ser negativo");
        }
    }

    public static void validarId(int id, String etiqueta) throws ProductoInvalidoException {
        if (id <= 0) {
            throw new ProductoInvalidoException(etiqueta + " debe ser un entero positivo");
        }
    }

    // ------------------------------------------------------------------
    // Conversión de texto a número
    // ------------------------------------------------------------------

    private static BigDecimal convertirDecimal(String texto, String etiqueta) throws ProductoInvalidoException {
        if (texto == null || texto.isBlank()) {
            throw new ProductoInvalidoException(etiqueta + " es obligatorio");
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException ex) {
            // Por ejemplo "abc" o "19,95" (con coma) no son decimales válidos.
            throw new ProductoInvalidoException(etiqueta + " no es un número decimal válido");
        }
    }

    private static int convertirEntero(String texto, String etiqueta) throws ProductoInvalidoException {
        if (texto == null || texto.isBlank()) {
            throw new ProductoInvalidoException(etiqueta + " es obligatorio");
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            // "12.5", "abc" o un número gigante no caben en un int.
            throw new ProductoInvalidoException(etiqueta + " debe ser un número entero");
        }
    }
}
