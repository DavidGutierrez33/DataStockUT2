package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.ficheros.ImportacionException;
import es.iesisidra.datastock.ficheros.ImportadorXml;
import es.iesisidra.datastock.ficheros.PersistenciaBinaria;
import es.iesisidra.datastock.ficheros.StockBinario;
import es.iesisidra.datastock.ficheros.StockBinario.RegistroStock;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
import es.iesisidra.datastock.util.SelectorRutas;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * RF-FIC-08: acceso aleatorio con RandomAccessFile.
 *
 * <p>Actividad INDEPENDIENTE del menú principal. Como es un programa aparte,
 * su "memoria" empieza vacía; para tener productos de los que hacer la copia
 * binaria, primero se cargan desde un catálogo XML o desde un .dat guardado
 * por DataStock (opción 1). Después stock.bin vive por su cuenta.</p>
 *
 * <p>Ejecución: mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.AccesoAleatorioActividad</p>
 */
public final class AccesoAleatorioActividad {

    private AccesoAleatorioActividad() { }

    public static void main(String[] args) {
        System.out.println(ColoresConsola.pintar("===== ACCESO ALEATORIO =====", ColoresConsola.NEGRITA));

        Inventario copiaEnMemoria = new Inventario(); // productos desde los que se generará stock.bin
        int opcion;
        do {
            System.out.println("\n1. Generar stock.bin | 2. Consultar posición | 3. Modificar stock"
                    + " | 4. Mostrar todos | 0. Salir");
            opcion = LectorConsola.leerOpcion("Opción: ", 0, 4);
            switch (opcion) {
                case 1 -> generar(copiaEnMemoria);
                case 2 -> consultar();
                case 3 -> modificar();
                case 4 -> mostrarTodos();
                default -> { }
            }
        } while (opcion != 0);
    }

    // ------------------------------------------------------------------
    // 1) Generar
    // ------------------------------------------------------------------

    private static void generar(Inventario memoria) {
        // Primero necesitamos productos en memoria.
        if (memoria.estaVacio()) {
            System.out.println("No hay productos en memoria. ¿De dónde los cargo?");
            System.out.println("  1. Catálogo XML   2. Inventario .dat   0. Cancelar");
            int origen = LectorConsola.leerOpcion("Opción: ", 0, 2);
            if (origen == 0) {
                return;
            }
            File fichero = SelectorRutas.pedirFicheroExistente("Ruta del fichero: ");
            if (fichero == null) {
                return;
            }
            try {
                if (origen == 1) {
                    ImportadorXml.importar(fichero, memoria);
                } else {
                    memoria.sustituirTodo(PersistenciaBinaria.recuperar(fichero));
                }
            } catch (ImportacionException | IOException ex) {
                System.out.println(ColoresConsola.pintar("ERROR al cargar productos: " + ex.getMessage(), ColoresConsola.ROJO));
                return;
            }
            if (memoria.estaVacio()) {
                System.out.println("El fichero no contenía productos válidos. No se genera stock.bin.");
                return;
            }
        }

        // destinoUtilizable() pide confirmación si stock.bin ya existe.
        File destino = SelectorRutas.pedirRutaDestino("Ruta de stock.bin (ej. datos/salidas/stock.bin): ");
        if (destino == null) {
            return;
        }
        try {
            int total = StockBinario.generar(memoria.getProductos(), destino);
            System.out.println("Fichero generado: " + total + " registros, "
                    + (total * StockBinario.TAMANO_REGISTRO) + " bytes.");
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("ERROR al escribir: " + ex.getMessage(), ColoresConsola.ROJO));
        }
    }

    // ------------------------------------------------------------------
    // 2) Consultar una posición (acceso directo con seek)
    // ------------------------------------------------------------------

    private static void consultar() {
        File fichero = SelectorRutas.pedirFicheroExistente("Fichero: ");
        if (fichero == null) {
            return;
        }
        try {
            int total = StockBinario.contarRegistros(fichero);
            System.out.println("Registros: " + total + " | Tamaño: " + fichero.length() + " bytes");
            if (total == 0) {
                return;
            }
            int posicion = LectorConsola.leerEntero("Consultar posición (0-" + (total - 1) + "): ");
            RegistroStock r = StockBinario.leer(fichero, posicion);
            System.out.println(r.codigo() + " | " + r.precio() + " € | stock " + r.stock());
        } catch (IllegalArgumentException ex) {
            // Posición fuera de rango.
            System.out.println(ColoresConsola.pintar(ex.getMessage(), ColoresConsola.AMARILLO));
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("ERROR: " + ex.getMessage(), ColoresConsola.ROJO));
        }
    }

    // ------------------------------------------------------------------
    // 3) Modificar solo el stock
    // ------------------------------------------------------------------

    private static void modificar() {
        File fichero = SelectorRutas.pedirFicheroExistente("Fichero: ");
        if (fichero == null) {
            return;
        }
        try {
            int total = StockBinario.contarRegistros(fichero);
            if (total == 0) {
                System.out.println("El fichero no tiene registros.");
                return;
            }
            int posicion = LectorConsola.leerEntero("Posición a modificar (0-" + (total - 1) + "): ");
            int nuevo = LectorConsola.leerEntero("Nuevo stock: ");
            int anterior = StockBinario.modificarStock(fichero, posicion, nuevo);
            System.out.println(ColoresConsola.pintar(
                    "Actualización correcta: stock " + anterior + " → " + nuevo, ColoresConsola.VERDE));
            System.out.println("(El inventario en memoria de DataStock NO cambia: stock.bin es una copia independiente.)");
        } catch (IllegalArgumentException ex) {
            System.out.println(ColoresConsola.pintar(ex.getMessage(), ColoresConsola.AMARILLO));
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("ERROR: " + ex.getMessage(), ColoresConsola.ROJO));
        }
    }

    // ------------------------------------------------------------------
    // 4) Lectura secuencial
    // ------------------------------------------------------------------

    private static void mostrarTodos() {
        File fichero = SelectorRutas.pedirFicheroExistente("Fichero: ");
        if (fichero == null) {
            return;
        }
        try {
            List<RegistroStock> registros = StockBinario.leerTodos(fichero);
            if (registros.isEmpty()) {
                System.out.println("El fichero no tiene registros.");
                return;
            }
            for (int i = 0; i < registros.size(); i++) {
                RegistroStock r = registros.get(i);
                System.out.println("[" + i + "] " + r.codigo() + " | " + r.precio() + " € | stock " + r.stock());
            }
        } catch (IOException ex) {
            System.out.println(ColoresConsola.pintar("ERROR: " + ex.getMessage(), ColoresConsola.ROJO));
        }
    }
}
