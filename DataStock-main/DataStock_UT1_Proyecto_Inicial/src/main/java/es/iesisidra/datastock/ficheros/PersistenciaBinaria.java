package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.validacion.ProductoInvalidoException;
import es.iesisidra.datastock.validacion.ValidadorProducto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RF-FIC-06: guarda y recupera el inventario con serialización de Java.
 *
 * <p>Serializar = convertir objetos Java en bytes (ObjectOutputStream).
 * Deserializar = reconstruir los objetos desde esos bytes (ObjectInputStream).
 * Producto implementa Serializable, por eso puede guardarse así.</p>
 *
 * <p>Ojo: deserializar un fichero de origen desconocido es PELIGROSO (puede
 * crear objetos arbitrarios). Por eso usamos un filtro que solo permite las
 * pocas clases que de verdad esperamos encontrar.</p>
 */
public final class PersistenciaBinaria {

    /**
     * Lista blanca de clases permitidas. Todo lo demás se rechaza ("!*").
     * BigDecimal arrastra a BigInteger y Number porque así se serializa por dentro.
     */
    private static final ObjectInputFilter FILTRO = ObjectInputFilter.Config.createFilter(
            "java.util.ArrayList;"
          + "java.lang.Object;"   // ArrayList guarda internamente un Object[]; hay que permitirlo
          + "es.iesisidra.datastock.modelo.Producto;"
          + "java.math.BigDecimal;java.math.BigInteger;java.lang.Number;java.lang.String;"
          + "maxdepth=10;maxrefs=1000000;maxarray=10000000;"
          + "!*");

    private PersistenciaBinaria() { }

    /**
     * Guarda la colección completa en un fichero .dat.
     *
     * @throws IOException si no se puede escribir
     */
    public static void guardar(List<Producto> productos, File destino) throws IOException {
        // Guardamos una COPIA en un ArrayList (que sí es Serializable) para
        // no depender del tipo concreto de lista que tenga el inventario.
        ArrayList<Producto> copia = new ArrayList<>(productos);

        // Buffered = escribe en bloques, más rápido. try-with-resources cierra todo.
        try (ObjectOutputStream salida = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(destino)))) {
            salida.writeObject(copia);
        }
    }

    /**
     * Lee un .dat y devuelve la lista de productos, ya comprobada.
     *
     * <p>NO modifica nada: devuelve una lista nueva. Quien la llame decide
     * si sustituye el inventario, y solo lo hará si aquí no hubo ningún error.</p>
     *
     * @throws IOException si el fichero está dañado, no es de DataStock o contiene datos no válidos
     */
    public static List<Producto> recuperar(File origen) throws IOException {
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(origen)))) {

            // El filtro se pone ANTES de leer ningún objeto.
            entrada.setObjectInputFilter(FILTRO);
            Object leido = entrada.readObject();

            // Comprobamos el tipo: tiene que ser una lista.
            if (!(leido instanceof List<?> lista)) {
                throw new IOException("el fichero no contiene una lista de productos");
            }

            List<Producto> productos = new ArrayList<>();
            Set<String> codigosVistos = new HashSet<>();
            for (Object elemento : lista) {
                // Cada elemento tiene que ser un Producto...
                if (!(elemento instanceof Producto producto)) {
                    throw new IOException("la lista contiene elementos que no son productos");
                }
                // ...y cumplir las mismas reglas que cualquier producto importado.
                try {
                    ValidadorProducto.validar(producto);
                } catch (ProductoInvalidoException ex) {
                    throw new IOException("producto no válido en el fichero: " + ex.getMessage());
                }
                // ...y no repetirse.
                if (!codigosVistos.add(producto.getCodigo().toUpperCase())) {
                    throw new IOException("código repetido en el fichero: " + producto.getCodigo());
                }
                productos.add(producto);
            }
            return productos;

        } catch (ClassNotFoundException ex) {
            // El fichero hace referencia a una clase que no existe.
            throw new IOException("el fichero no es un inventario de DataStock", ex);
        } catch (EOFException ex) {
            // El fichero se corta antes de tiempo (EOFException no trae mensaje, ponemos uno nuestro).
            throw new IOException("el fichero está incompleto o dañado", ex);
        } catch (InvalidClassException ex) {
            // Incluye los objetos rechazados por el filtro de seguridad.
            throw new IOException("el fichero contiene datos no permitidos o de otra versión", ex);
        }
        // Otros fallos (StreamCorruptedException...) ya son IOException: suben tal cual.
    }
}
