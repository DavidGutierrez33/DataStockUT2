package es.iesisidra.datastock.ficheros;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Lee ficheros XML con DOM de forma SEGURA.
 *
 * <p>¿Qué es XXE? Un XML puede traer una declaración DOCTYPE (DTD) que define
 * "entidades externas". Un atacante podría usarlas para que nuestro programa
 * lea ficheros del ordenador (por ejemplo /etc/passwd) o haga peticiones de
 * red sin que lo sepamos. La defensa más simple y fuerte es PROHIBIR el
 * DOCTYPE por completo: si el documento lo trae, se rechaza.</p>
 *
 * <p>La usan la importación XML (RF-FIC-03) y las consultas XPath (RF-FIC-07),
 * así la configuración segura está en un único sitio.</p>
 */
public final class XmlSeguro {

    private XmlSeguro() { }

    /**
     * Abre un fichero XML y devuelve el árbol DOM.
     *
     * @throws ImportacionException si el fichero no se puede leer, está mal
     *         formado o contiene un DTD. El mensaje es apto para el usuario.
     */
    public static Document leer(File fichero) throws ImportacionException {
        try {
            DocumentBuilder constructor = crearConstructor();
            // parse() lee el fichero entero y construye el árbol de nodos en memoria.
            return constructor.parse(fichero);

        } catch (SAXParseException ex) {
            // Error de sintaxis XML. Si el motivo es el DOCTYPE lo explicamos aparte.
            String detalle = ex.getMessage() == null ? "" : ex.getMessage();
            if (detalle.contains("DOCTYPE")) {
                throw new ImportacionException(
                        "El documento contiene una declaración DOCTYPE/DTD. Se rechaza por seguridad (XXE).", ex);
            }
            throw new ImportacionException(
                    "XML mal formado (línea " + ex.getLineNumber() + "): " + detalle, ex);
        } catch (SAXException ex) {
            throw new ImportacionException("No se ha podido interpretar el XML: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ImportacionException("No se ha podido leer el fichero: " + ex.getMessage(), ex);
        } catch (ParserConfigurationException ex) {
            throw new ImportacionException("Error al configurar el analizador XML: " + ex.getMessage(), ex);
        }
    }

    /** Crea un DocumentBuilder con todas las protecciones activadas. */
    private static DocumentBuilder crearConstructor() throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();

        // 1) Prohibir cualquier DOCTYPE. Es la medida principal contra XXE.
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        // 2) Medidas de refuerzo: sin entidades externas ni XInclude.
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);

        DocumentBuilder constructor = fabrica.newDocumentBuilder();

        // Por defecto Java imprime "[Fatal Error]..." por la consola al fallar.
        // Con este manejador lanzamos la excepción y NO imprimimos nada nosotros:
        // decidimos nosotros cómo informar al usuario.
        constructor.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException ex) { /* los avisos se ignoran */ }
            @Override public void error(SAXParseException ex) throws SAXException { throw ex; }
            @Override public void fatalError(SAXParseException ex) throws SAXException { throw ex; }
        });
        return constructor;
    }
}
