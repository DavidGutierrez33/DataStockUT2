package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.ficheros.ImportacionException;
import es.iesisidra.datastock.ficheros.XmlSeguro;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.util.SelectorRutas;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.math.BigDecimal;

/**
 * RF-FIC-07: consultas XPath sobre un catálogo XML.
 *
 * <p>Es una actividad INDEPENDIENTE: tiene su propio main y no usa el
 * inventario. Abre el XML una sola vez y permite hacer varias consultas.</p>
 *
 * <p>XPath es un lenguaje para "preguntar" a un documento XML. Ejemplos:</p>
 * <ul>
 *   <li>/catalogo/producto → todos los productos</li>
 *   <li>/catalogo/producto[categoriaId=1] → los de la categoría 1</li>
 *   <li>/catalogo/producto[precio &gt; 100] → los de precio mayor que 100</li>
 *   <li>count(/catalogo/producto) → cuántos productos hay</li>
 * </ul>
 *
 * <p>Ejecución: mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.XPathActividad</p>
 */
public final class XPathActividad {

    private XPathActividad() { }

    public static void main(String[] args) {
        System.out.println(ColoresConsola.pintar("===== CONSULTAS XPATH =====", ColoresConsola.NEGRITA));

        // 1) Pedir y abrir el XML (con la misma lectura segura que la importación: sin DTD).
        File fichero = SelectorRutas.pedirFicheroExistente("Fichero: ");
        if (fichero == null) {
            return; // el motivo ya se ha mostrado
        }
        Document documento;
        try {
            documento = XmlSeguro.leer(fichero);
        } catch (ImportacionException ex) {
            System.out.println(ColoresConsola.pintar("ERROR: " + ex.getMessage(), ColoresConsola.ROJO));
            return;
        }
        if (!"catalogo".equals(documento.getDocumentElement().getTagName())) {
            System.out.println(ColoresConsola.pintar(
                    "ERROR: la raíz debe ser <catalogo>.", ColoresConsola.ROJO));
            return;
        }

        // 2) Preparar XPath una vez; se reutiliza en todas las consultas.
        XPath xpath = XPathFactory.newInstance().newXPath();

        // 3) Bucle de consultas sobre el documento ya abierto.
        int opcion;
        do {
            System.out.println("\n1. Por código | 2. Por categoría | 3. Precio superior | 4. Contar | 0. Salir");
            opcion = LectorConsola.leerOpcion("Opción: ", 0, 4);
            try {
                switch (opcion) {
                    case 1 -> consultarPorCodigo(xpath, documento);
                    case 2 -> consultarPorCategoria(xpath, documento);
                    case 3 -> consultarPorPrecio(xpath, documento);
                    case 4 -> contar(xpath, documento);
                    default -> { }
                }
            } catch (XPathExpressionException ex) {
                System.out.println(ColoresConsola.pintar(
                        "ERROR en la consulta XPath: " + ex.getMessage(), ColoresConsola.ROJO));
            }
        } while (opcion != 0);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Por código. NO metemos el texto del usuario dentro de la expresión
     * XPath (sería una "inyección XPath"): seleccionamos todos los productos
     * y comparamos el código con equalsIgnoreCase en Java.
     */
    private static void consultarPorCodigo(XPath xpath, Document doc) throws XPathExpressionException {
        String buscado = LectorConsola.leerTexto("Código: ").trim();
        if (buscado.isEmpty()) {
            System.out.println(ColoresConsola.pintar("El código no puede estar vacío.", ColoresConsola.AMARILLO));
            return;
        }
        NodeList todos = (NodeList) xpath.evaluate("/catalogo/producto", doc, XPathConstants.NODESET);
        int encontrados = 0;
        for (int i = 0; i < todos.getLength(); i++) {
            Element producto = (Element) todos.item(i);
            // Dentro de un nodo, una ruta relativa ("codigo") busca entre sus hijos.
            String codigo = xpath.evaluate("codigo", producto).trim();
            if (codigo.equalsIgnoreCase(buscado)) {
                mostrar(xpath, producto);
                encontrados++;
            }
        }
        resumen(encontrados);
    }

    /** Por categoría: el valor se valida como entero antes de usarlo en la expresión. */
    private static void consultarPorCategoria(XPath xpath, Document doc) throws XPathExpressionException {
        int categoria;
        try {
            categoria = Integer.parseInt(LectorConsola.leerTexto("Categoría: ").trim());
        } catch (NumberFormatException ex) {
            System.out.println(ColoresConsola.pintar("La categoría debe ser un número entero.", ColoresConsola.AMARILLO));
            return;
        }
        // Es seguro concatenar: "categoria" es un int, solo contiene dígitos (y quizá '-').
        NodeList nodos = (NodeList) xpath.evaluate(
                "/catalogo/producto[categoriaId=" + categoria + "]", doc, XPathConstants.NODESET);
        mostrarLista(xpath, nodos);
    }

    /** Precio ESTRICTAMENTE superior (el valor igual al límite queda fuera). */
    private static void consultarPorPrecio(XPath xpath, Document doc) throws XPathExpressionException {
        BigDecimal minimo;
        try {
            minimo = new BigDecimal(LectorConsola.leerTexto("Precio mínimo: ").trim());
        } catch (NumberFormatException ex) {
            System.out.println(ColoresConsola.pintar("El precio debe ser un número (usa punto decimal).", ColoresConsola.AMARILLO));
            return;
        }
        // toPlainString() garantiza solo dígitos y punto, sin notación científica.
        NodeList nodos = (NodeList) xpath.evaluate(
                "/catalogo/producto[precio > " + minimo.toPlainString() + "]", doc, XPathConstants.NODESET);
        mostrarLista(xpath, nodos);
    }

    /** count() cuenta los elementos <producto> aunque alguno tenga datos incorrectos. */
    private static void contar(XPath xpath, Document doc) throws XPathExpressionException {
        // count() devuelve un número; pedimos el resultado como Double.
        Double total = (Double) xpath.evaluate("count(/catalogo/producto)", doc, XPathConstants.NUMBER);
        System.out.println("Productos en el catálogo: " + total.intValue());
    }

    // ------------------------------------------------------------------
    // Mostrar resultados
    // ------------------------------------------------------------------

    private static void mostrarLista(XPath xpath, NodeList nodos) throws XPathExpressionException {
        for (int i = 0; i < nodos.getLength(); i++) {
            mostrar(xpath, (Element) nodos.item(i));
        }
        resumen(nodos.getLength());
    }

    /** Escribe una línea tipo: MON-003 Monitor 24 pulgadas 149.99 € */
    private static void mostrar(XPath xpath, Element producto) throws XPathExpressionException {
        String codigo = xpath.evaluate("codigo", producto).trim();
        String nombre = xpath.evaluate("nombre", producto).trim();
        String precio = xpath.evaluate("precio", producto).trim();
        System.out.println(codigo + " " + nombre + " " + precio + " €");
    }

    private static void resumen(int total) {
        if (total == 0) {
            System.out.println(ColoresConsola.pintar("No hay coincidencias.", ColoresConsola.AMARILLO));
        } else {
            System.out.println("Productos encontrados: " + total);
        }
    }
}
