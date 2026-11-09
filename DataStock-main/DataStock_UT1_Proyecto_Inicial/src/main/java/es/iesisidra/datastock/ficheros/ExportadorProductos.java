package es.iesisidra.datastock.ficheros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import es.iesisidra.datastock.modelo.Producto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RF-FIC-05: exporta el inventario a JSON (Gson) y a XML (DOM).
 *
 * <p>No hay conversor directo JSON↔XML. El "puente" es la colección Java:
 * JSON → Producto → XML, y XML → Producto → JSON.</p>
 *
 * <p>Esta clase solo ESCRIBE. Las comprobaciones con el usuario (inventario
 * vacío, confirmar sobrescritura...) las hace el menú antes de llamarla.</p>
 */
public final class ExportadorProductos {

    private ExportadorProductos() { }

    // ------------------------------------------------------------------
    // JSON
    // ------------------------------------------------------------------

    /**
     * Clase auxiliar para que el JSON tenga el objeto raíz {"productos": [...]}.
     * Gson usa el NOMBRE del atributo como clave del JSON, por eso se llama
     * igual que en el formato del enunciado.
     */
    private static class CatalogoJson {
        private final List<Producto> productos;
        CatalogoJson(List<Producto> productos) { this.productos = productos; }
    }

    /**
     * Escribe el JSON en UTF-8, con productos ordenados por código.
     *
     * @throws IOException si no se puede escribir el fichero
     */
    public static void exportarJson(List<Producto> productos, File destino) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()      // JSON con sangrías, legible
                .disableHtmlEscaping()    // evita que Gson cambie símbolos como < > & '
                .create();

        // try-with-resources: el Writer se cierra solo, aunque haya error.
        try (Writer escritor = Files.newBufferedWriter(destino.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(new CatalogoJson(ordenadosPorCodigo(productos)), escritor);
        }
    }

    // ------------------------------------------------------------------
    // XML
    // ------------------------------------------------------------------

    /**
     * Construye el árbol DOM y lo guarda en UTF-8 con indentación.
     *
     * @throws IOException si falla la escritura o la generación del XML
     */
    public static void exportarXml(List<Producto> productos, File destino) throws IOException {
        try {
            // 1) Crear un documento vacío en memoria.
            Document documento = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();

            // 2) Elemento raíz <catalogo>.
            Element raiz = documento.createElement("catalogo");
            documento.appendChild(raiz);

            // 3) Un <producto> por cada producto, con sus seis hijos.
            for (Producto p : ordenadosPorCodigo(productos)) {
                Element elemento = documento.createElement("producto");
                anadirCampo(documento, elemento, "codigo", p.getCodigo());
                anadirCampo(documento, elemento, "nombre", p.getNombre());
                // toPlainString evita notaciones científicas como 1E+2
                anadirCampo(documento, elemento, "precio", p.getPrecio().toPlainString());
                anadirCampo(documento, elemento, "stock", String.valueOf(p.getStock()));
                anadirCampo(documento, elemento, "categoriaId", String.valueOf(p.getCategoriaId()));
                anadirCampo(documento, elemento, "proveedorId", String.valueOf(p.getProveedorId()));
                raiz.appendChild(elemento);
            }

            // 4) Guardar el árbol en un fichero. El "Transformer" convierte
            //    un árbol DOM en texto XML.
            TransformerFactory fabrica = TransformerFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformador = fabrica.newTransformer();
            transformador.setOutputProperty(OutputKeys.INDENT, "yes");
            transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformador.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            // La cabecera <?xml ...?> la escribimos nosotros a mano (con salto de línea):
            // el Transformer la pegaría a <catalogo> en la misma línea.
            transformador.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            try (OutputStream salida = Files.newOutputStream(destino.toPath())) {
                salida.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes(StandardCharsets.UTF_8));
                transformador.transform(new DOMSource(documento), new StreamResult(salida));
            }
        } catch (ParserConfigurationException | TransformerException ex) {
            // Convertimos estos errores en IOException para que el menú
            // solo tenga que capturar un tipo.
            throw new IOException("No se ha podido generar el XML: " + ex.getMessage(), ex);
        }
    }

    /** Crea <etiqueta>texto</etiqueta> y lo cuelga del elemento padre. */
    private static void anadirCampo(Document documento, Element padre, String etiqueta, String texto) {
        Element hijo = documento.createElement(etiqueta);
        hijo.setTextContent(texto); // DOM escapa solo caracteres como & o <
        padre.appendChild(hijo);
    }

    // ------------------------------------------------------------------

    /** Copia la lista y la ordena por código (la original no se toca). */
    private static List<Producto> ordenadosPorCodigo(List<Producto> productos) {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort(Comparator.comparing(Producto::getCodigo, String.CASE_INSENSITIVE_ORDER));
        return copia;
    }
}
