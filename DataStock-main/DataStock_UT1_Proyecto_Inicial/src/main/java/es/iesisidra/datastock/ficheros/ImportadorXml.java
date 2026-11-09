package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.validacion.ProductoInvalidoException;
import es.iesisidra.datastock.validacion.ValidadorProducto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

/**
 * RF-FIC-03: importa un catálogo XML al inventario usando DOM.
 *
 * <p>DOM carga TODO el documento en memoria como un árbol de nodos
 * (Document → Element → Element...). Después lo recorremos con NodeList.</p>
 *
 * <p>Estructura esperada:</p>
 * <pre>
 * &lt;catalogo&gt;
 *   &lt;producto&gt;
 *     &lt;codigo&gt;TEC-001&lt;/codigo&gt; &lt;nombre&gt;...&lt;/nombre&gt; ...
 *   &lt;/producto&gt;
 * &lt;/catalogo&gt;
 * </pre>
 *
 * <p>Los tres tipos de problema del apartado 6 del enunciado:</p>
 * <ol>
 *   <li>Documento roto o raíz incorrecta → ImportacionException (no se importa NADA).</li>
 *   <li>Producto con datos incorrectos → se rechaza ese producto y se sigue.</li>
 *   <li>Producto duplicado → se rechaza, el existente no se toca.</li>
 * </ol>
 */
public final class ImportadorXml {

    private ImportadorXml() { }

    /**
     * Importa los productos válidos del fichero.
     *
     * @param fichero    XML a leer
     * @param inventario colección común a la que se añaden los productos
     * @return resumen con encontrados, importados y rechazados
     * @throws ImportacionException si el documento en sí no es utilizable;
     *         en ese caso el inventario queda SIN cambios
     */
    public static ResumenImportacion importar(File fichero, Inventario inventario)
            throws ImportacionException {

        // PASO 1: leer el documento. Si falla (mal formado, DTD...) salta la
        // excepción AQUÍ, antes de tocar el inventario → importación cancelada.
        Document documento = XmlSeguro.leer(fichero);

        // PASO 2: comprobar que la etiqueta raíz es <catalogo>.
        Element raiz = documento.getDocumentElement();
        if (!"catalogo".equals(raiz.getTagName())) {
            throw new ImportacionException(
                    "La raíz del documento debe ser <catalogo> y es <" + raiz.getTagName() + ">.");
        }

        // PASO 3: recorrer todos los <producto>.
        NodeList nodos = raiz.getElementsByTagName("producto");
        ResumenImportacion resumen = new ResumenImportacion();

        for (int i = 0; i < nodos.getLength(); i++) {
            Element elemento = (Element) nodos.item(i);
            resumen.registrarEncontrado();

            // Leemos los seis campos como TEXTO (null si falta la etiqueta).
            String codigo      = leerCampo(elemento, "codigo");
            String nombre      = leerCampo(elemento, "nombre");
            String precio      = leerCampo(elemento, "precio");
            String stock       = leerCampo(elemento, "stock");
            String categoriaId = leerCampo(elemento, "categoriaId");
            String proveedorId = leerCampo(elemento, "proveedorId");

            // Etiqueta para identificar el producto en las incidencias.
            String etiqueta = "Producto " + (i + 1);
            if (codigo != null && !codigo.isBlank()) {
                etiqueta += " [" + codigo.trim() + "]";
            }

            try {
                // Convierte y valida (mismas reglas que JSON). Lanza excepción si falla.
                Producto producto = ValidadorProducto.crearDesdeTextos(
                        codigo, nombre, precio, stock, categoriaId, proveedorId);

                // agregar() devuelve false si el código ya existía.
                if (!inventario.agregar(producto)) {
                    throw new ProductoInvalidoException("código duplicado (ya existe en el inventario)");
                }
                resumen.registrarImportado();

            } catch (ProductoInvalidoException ex) {
                // Producto rechazado: anotamos el motivo y CONTINUAMOS con el siguiente.
                resumen.registrarRechazo(etiqueta + ": " + ex.getMessage());
            }
        }
        return resumen;
    }

    /**
     * Devuelve el texto de la primera etiqueta hija con ese nombre,
     * o null si el producto no la tiene.
     */
    private static String leerCampo(Element padre, String etiqueta) {
        NodeList lista = padre.getElementsByTagName(etiqueta);
        if (lista.getLength() == 0) {
            return null;
        }
        return lista.item(0).getTextContent();
    }
}
