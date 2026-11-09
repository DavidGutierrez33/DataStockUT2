package es.iesisidra.datastock.servicio;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.validacion.ProductoInvalidoException;
import es.iesisidra.datastock.validacion.ValidadorProducto;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Estado común del inventario durante la ejecución de UT1.
 *
 * <p>Es la ÚNICA colección de productos del programa: la importan JSON y XML,
 * la consultan las búsquedas, la exportan y la serializan. Aquí viven las
 * reglas de "alta, búsqueda y sustitución" que pide el README.</p>
 */
public class Inventario {
   private static final List<Producto> productos = new ArrayList<>();

    /**
     * Obtiene la colección que comparten las opciones del menú.
     * (Se mantiene tal cual venía en el proyecto base.)
     */
    public List<Producto> getProductos() { return productos; }

    public static boolean existeProducto(String codigo) {
        for (int i = 0; i < productos.size(); i++) {
            if (productos.get(i).getCodigo().equals(codigo)) {
                return true; // Ya existe
            }
        }
        return false; // No existe
    }

    public static void gestionarImportacionJson() {
        String rutaInput = LectorConsola.leerTexto("Ruta del fichero JSON: ").trim();
        File fichero = new File(rutaInput);

        if (!fichero.exists() || !fichero.isFile()) {
            ColoresConsola.error("El archivo no existe o no es un fichero.");
            LectorConsola.pausar();
            return; // Si el archivo no existe cortamos aquí
        }

        int encontrados = 0;
        int importados = 0;
        int rechazados = 0;

        try {
            String contenidoJson = new String(Files.readAllBytes(Paths.get(rutaInput)), StandardCharsets.UTF_8);
            // Los catálogos de ejemplo tienen la forma {"productos": [ ... ]}; también aceptamos un array suelto
            String texto = contenidoJson.trim();
            JSONArray jsonArray = texto.startsWith("[")
                    ? new JSONArray(texto)
                    : new JSONObject(texto).getJSONArray("productos");
            encontrados = jsonArray.length();

            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    // Mismas reglas que el XML: ValidadorProducto convierte y comprueba cada campo
                    Producto nuevo = ValidadorProducto.crearDesdeTextos(
                            campo(jsonObject, "codigo", "codigo"),
                            campo(jsonObject, "nombre", "nombre"),
                            campo(jsonObject, "precio", "precio"),
                            campo(jsonObject, "stock", "stock"),
                            campo(jsonObject, "categoriaId", "categoria_id"),
                            campo(jsonObject, "proveedorId", "proveedor_id"));

                    // Evitar códigos duplicados en memoria
                    if (existeProducto(nuevo.getCodigo())) {
                        System.out.println("Rechazado (posición " + i + "): el código " + nuevo.getCodigo() + " ya está en memoria.");
                        rechazados++;
                        continue;
                    }
                    productos.add(nuevo);
                    importados++;
                } catch (ProductoInvalidoException e) {
                    System.out.println("Rechazado (posición " + i + "): " + e.getMessage());
                    rechazados++;
                } catch (Exception e) {
                    System.out.println("Rechazado (posición " + i + "): el elemento no es un objeto JSON válido.");
                    rechazados++;
                }
            }

            ColoresConsola.ok("Fichero JSON procesado correctamente.");
            System.out.println("Productos encontrados: " + encontrados);
            System.out.println("Productos importados:  " + importados);
            System.out.println("Productos rechazados:  " + rechazados);
            System.out.println("Productos disponibles en memoria: " + productos.size());

        } catch (Exception e) {
            ColoresConsola.error("La estructura general del archivo JSON es incorrecta. No se ha importado nada.");
        }
        LectorConsola.pausar();
    }

    /** Devuelve el campo como texto (o null si falta), probando el nombre camelCase y el snake_case. */
    private static String campo(JSONObject objeto, String nombre, String alternativo) {
        Object valor = objeto.has(nombre) ? objeto.get(nombre) : objeto.opt(alternativo);
        return (valor == null || valor == JSONObject.NULL) ? null : valor.toString();
    }

    /** @return true si no hay ningún producto en memoria. */
    public boolean estaVacio() { return productos.isEmpty(); }

    /**
     * Comprueba si ya existe un producto con ese código.
     * Ignora mayúsculas/minúsculas y espacios extremos: "tec-001 " == "TEC-001".
     */
    public boolean existeCodigo(String codigo) {
        return buscarPorCodigo(codigo).isPresent();
    }

    /**
     * REGLA DE ALTA: añade el producto solo si su código no existe.
     * No sustituye ni modifica el producto que ya estuviera.
     *
     * @return true si se ha añadido; false si era un duplicado.
     */
    public boolean agregar(Producto nuevo) {
        if (existeCodigo(nuevo.getCodigo())) {
            return false;
        }
        productos.add(nuevo);
        return true;
    }

    /**
     * Busca por código exacto (sin distinguir mayúsculas ni espacios extremos).
     * Optional es una "caja" que puede contener un producto o estar vacía;
     * es más seguro que devolver null porque obliga a pensar en el caso
     * "no encontrado".
     */
    public Optional<Producto> buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        String buscado = codigo.trim();
        for (Producto p : productos) {
            if (p.getCodigo().equalsIgnoreCase(buscado)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    /**
     * Devuelve los productos cuyo nombre CONTIENE el texto, sin distinguir
     * mayúsculas. El resultado va ordenado por código.
     */
    public List<Producto> buscarPorNombre(String fragmento) {
        List<Producto> coincidencias = new ArrayList<>();
        if (fragmento == null || fragmento.isBlank()) {
            return coincidencias; // un texto vacío no busca nada
        }
        String buscado = fragmento.trim().toLowerCase();
        for (Producto p : productos) {
            if (p.getNombre().toLowerCase().contains(buscado)) {
                coincidencias.add(p);
            }
        }
        coincidencias.sort(porCodigo());
        return coincidencias;
    }

    /**
     * Devuelve una COPIA de la lista ordenada por código.
     * Se ordena la copia y no la original, así las consultas NO alteran la
     * colección (requisito de RF-FIC-04).
     */
    public List<Producto> listarOrdenadosPorCodigo() {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort(porCodigo());
        return copia;
    }

    /**
     * Sustituye TODO el contenido por los productos indicados.
     * Se usa al recuperar un .dat (RF-FIC-06). Se llama solo cuando la
     * recuperación ya ha terminado bien, para no perder datos si falla.
     */
    public void sustituirTodo(Collection<Producto> nuevos) {
        productos.clear();
        productos.addAll(nuevos);
    }

    /** Comparador por código ignorando mayúsculas (para ordenar listas). */
    private static Comparator<Producto> porCodigo() {
        return Comparator.comparing(Producto::getCodigo, String.CASE_INSENSITIVE_ORDER);
    }
}
