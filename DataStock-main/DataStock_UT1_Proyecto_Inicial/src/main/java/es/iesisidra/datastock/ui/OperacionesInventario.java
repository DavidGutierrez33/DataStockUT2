package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.ficheros.ExportadorProductos;
import es.iesisidra.datastock.ficheros.ImportacionException;
import es.iesisidra.datastock.ficheros.ImportadorXml;
import es.iesisidra.datastock.ficheros.PersistenciaBinaria;
import es.iesisidra.datastock.ficheros.ResumenImportacion;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.util.SelectorRutas;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Une el menú con la lógica: cada método es lo que ocurre al elegir UNA opción
 * (pide datos al usuario, llama a la clase que hace el trabajo y muestra el resultado).
 *
 * <p>Así el switch de MenuPrincipal solo tiene que llamar a un método, tal y
 * como exige el enunciado. Aquí NO se lee/escribe ningún fichero directamente:
 * eso lo hacen ImportadorXml, ExportadorProductos y PersistenciaBinaria.</p>
 */
public class OperacionesInventario {
    private final Inventario inventario;

    public OperacionesInventario(Inventario inventario) {
        this.inventario = inventario;
    }

    // ------------------------------------------------------------------
    // RF-FIC-03 · Importar XML
    // ------------------------------------------------------------------

    public void importarXml() {
        System.out.println(ColoresConsola.pintar("\n===== IMPORTAR XML =====", ColoresConsola.NEGRITA));
        File fichero = SelectorRutas.pedirFicheroExistente("Ruta: ");
        if (fichero == null) {
            return;
        }
        System.out.println("Leyendo documento XML...");
        try {
            ResumenImportacion resumen = ImportadorXml.importar(fichero, inventario);
            resumen.imprimir();
        } catch (ImportacionException ex) {
            // Error global: no se ha importado nada y el inventario sigue igual.
            System.out.println(ColoresConsola.pintar("ERROR: " + ex.getMessage(), ColoresConsola.ROJO));
            System.out.println("Importación cancelada. El inventario no ha cambiado.");
        }
    }

    // ------------------------------------------------------------------
    // RF-FIC-04 · Consultas
    // ------------------------------------------------------------------

    public void listar() {
        System.out.println(ColoresConsola.pintar("\n===== LISTADO DE PRODUCTOS =====", ColoresConsola.NEGRITA));
        if (inventario.estaVacio()) {
            System.out.println(ColoresConsola.pintar("El inventario está vacío. Importa un catálogo primero.", ColoresConsola.AMARILLO));
            return;
        }
        List<Producto> lista = inventario.listarOrdenadosPorCodigo();
        for (Producto p : lista) {
            // Una línea por producto con los datos principales.
            System.out.printf("%-12s %-28s %9s € | Stock: %4d | Cat: %d | Prov: %d%n",
                    p.getCodigo(), p.getNombre(), p.getPrecio().toPlainString(),
                    p.getStock(), p.getCategoriaId(), p.getProveedorId());
        }
        System.out.println("Total: " + lista.size() + " productos.");
    }

    public void buscarPorCodigo() {
        if (inventario.estaVacio()) {
            System.out.println(ColoresConsola.pintar("El inventario está vacío. Importa un catálogo primero.", ColoresConsola.AMARILLO));
            return;
        }
        String codigo = LectorConsola.leerTexto("Código: ").trim();
        if (codigo.isEmpty()) {
            System.out.println(ColoresConsola.pintar("Debes escribir un código.", ColoresConsola.AMARILLO));
            return;
        }
        Optional<Producto> resultado = inventario.buscarPorCodigo(codigo);
        if (resultado.isPresent()) {
            Producto p = resultado.get();
            System.out.println("Código: " + p.getCodigo());
            System.out.println("Nombre: " + p.getNombre());
            System.out.println("Precio: " + p.getPrecio().toPlainString() + " € | Stock: " + p.getStock());
            System.out.println("Categoría: " + p.getCategoriaId() + " | Proveedor: " + p.getProveedorId());
        } else {
            System.out.println(ColoresConsola.pintar("No existe ningún producto con el código " + codigo.toUpperCase() + ".", ColoresConsola.AMARILLO));
        }
    }

    public void buscarPorNombre() {
        if (inventario.estaVacio()) {
            System.out.println(ColoresConsola.pintar("El inventario está vacío. Importa un catálogo primero.", ColoresConsola.AMARILLO));
            return;
        }
        String texto = LectorConsola.leerTexto("Texto a buscar en el nombre: ").trim();
        if (texto.isEmpty()) {
            System.out.println(ColoresConsola.pintar("Debes escribir un texto de búsqueda.", ColoresConsola.AMARILLO));
            return;
        }
        List<Producto> coincidencias = inventario.buscarPorNombre(texto);
        if (coincidencias.isEmpty()) {
            System.out.println(ColoresConsola.pintar("No hay productos cuyo nombre contenga \"" + texto + "\".", ColoresConsola.AMARILLO));
            return;
        }
        for (Producto p : coincidencias) {
            System.out.printf("%-12s %-28s %9s € | Stock: %d%n",
                    p.getCodigo(), p.getNombre(), p.getPrecio().toPlainString(), p.getStock());
        }
        System.out.println("Coincidencias: " + coincidencias.size());
    }

    // ------------------------------------------------------------------
    // RF-FIC-05 · Exportar
    // ------------------------------------------------------------------

    public void exportarJson() {
        exportar("JSON");
    }

    public void exportarXml() {
        exportar("XML");
    }

    /** Las comprobaciones son idénticas para los dos formatos: solo cambia quién escribe. */
    private void exportar(String formato) {
        System.out.println(ColoresConsola.pintar("\n===== EXPORTAR A " + formato + " =====", ColoresConsola.NEGRITA));
        // 1) Inventario no vacío.
        if (inventario.estaVacio()) {
            System.out.println(ColoresConsola.pintar("No hay productos que exportar.", ColoresConsola.AMARILLO));
            return;
        }
        // 2) Ruta válida + confirmación de sobrescritura (todo en SelectorRutas).
        File destino = SelectorRutas.pedirRutaDestino("Ruta (ej. datos/salidas/inventario." + formato.toLowerCase() + "): ");
        if (destino == null) {
            return;
        }
        // 3) Escribir.
        try {
            if (formato.equals("JSON")) {
                ExportadorProductos.exportarJson(inventario.getProductos(), destino);
            } else {
                ExportadorProductos.exportarXml(inventario.getProductos(), destino);
            }
            System.out.println(ColoresConsola.pintar("Fichero generado correctamente.", ColoresConsola.VERDE));
            System.out.println("Ruta: " + destino.getAbsolutePath() + " | Formato: " + formato);
            System.out.println("Productos exportados: " + inventario.getProductos().size());
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("ERROR al escribir el fichero: " + ex.getMessage(), ColoresConsola.ROJO));
        }
    }

    // ------------------------------------------------------------------
    // RF-FIC-06 · Serialización
    // ------------------------------------------------------------------

    public void guardarDat() {
        System.out.println(ColoresConsola.pintar("\n===== GUARDAR INVENTARIO =====", ColoresConsola.NEGRITA));
        if (inventario.estaVacio()) {
            System.out.println(ColoresConsola.pintar("No hay productos que guardar. No se crea ningún fichero.", ColoresConsola.AMARILLO));
            return;
        }
        File destino = SelectorRutas.pedirRutaDestino("Ruta (ej. datos/salidas/inventario.dat): ");
        if (destino == null) {
            return;
        }
        try {
            PersistenciaBinaria.guardar(inventario.getProductos(), destino);
            System.out.println(ColoresConsola.pintar("Inventario guardado correctamente.", ColoresConsola.VERDE));
            System.out.println("Productos guardados: " + inventario.getProductos().size());
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("ERROR al guardar: " + ex.getMessage(), ColoresConsola.ROJO));
        }
    }

    public void recuperarDat() {
        System.out.println(ColoresConsola.pintar("\n===== RECUPERAR INVENTARIO =====", ColoresConsola.NEGRITA));
        File origen = SelectorRutas.pedirFicheroExistente("Ruta: ");
        if (origen == null) {
            return;
        }
        // Aviso y confirmación ANTES de tocar nada, si ya hay productos.
        if (!inventario.estaVacio()) {
            System.out.println("Actualmente hay " + inventario.getProductos().size() + " productos en memoria.");
            System.out.println("La recuperación sustituirá el inventario actual.");
            if (!LectorConsola.confirmar("¿Continuar?")) {
                System.out.println("Operación cancelada. Se conserva el inventario actual.");
                return;
            }
        }
        try {
            // Se lee y valida TODO primero. Si algo falla, salta la excepción y la memoria no se toca.
            List<Producto> recuperados = PersistenciaBinaria.recuperar(origen);
            inventario.sustituirTodo(recuperados);
            System.out.println(ColoresConsola.pintar("Inventario recuperado correctamente.", ColoresConsola.VERDE));
            System.out.println("Productos disponibles: " + inventario.getProductos().size());
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("ERROR: no se puede recuperar el fichero (" + ex.getMessage()
                    + "). El inventario no ha cambiado.", ColoresConsola.ROJO));
        }
    }
}
