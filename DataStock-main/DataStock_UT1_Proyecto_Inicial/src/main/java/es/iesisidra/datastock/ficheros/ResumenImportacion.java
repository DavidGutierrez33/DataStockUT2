package es.iesisidra.datastock.ficheros;

import es.iesisidra.datastock.util.ColoresConsola;

import java.util.ArrayList;
import java.util.List;

/**
 * Lleva la cuenta de una importación: cuántos productos había, cuántos se
 * han añadido, cuántos se han rechazado y por qué.
 *
 * <p>La usa la importación XML y también podrá usarla la de JSON, de modo que
 * el resumen final es idéntico en ambos casos.</p>
 */
public class ResumenImportacion {
    private int encontrados;
    private int importados;
    private final List<String> incidencias = new ArrayList<>();

    /** Llamar una vez por cada producto que aparece en el fichero. */
    public void registrarEncontrado() { encontrados++; }

    /** Llamar cuando un producto se ha añadido al inventario. */
    public void registrarImportado() { importados++; }

    /** Llamar cuando un producto se rechaza; guarda el motivo para mostrarlo. */
    public void registrarRechazo(String incidencia) { incidencias.add(incidencia); }

    public int getEncontrados() { return encontrados; }
    public int getImportados() { return importados; }
    /** Los rechazados son simplemente tantos como incidencias hay. */
    public int getRechazados() { return incidencias.size(); }
    public List<String> getIncidencias() { return incidencias; }

    /** Muestra el resumen con el formato del enunciado. */
    public void imprimir() {
        System.out.println("Productos encontrados: " + encontrados);
        System.out.println("Importados: " + importados + " | Rechazados: " + getRechazados());
        if (!incidencias.isEmpty()) {
            System.out.println("INCIDENCIAS:");
            for (String incidencia : incidencias) {
                System.out.println(ColoresConsola.pintar("- " + incidencia, ColoresConsola.AMARILLO));
            }
            System.out.println("Importación finalizada con incidencias.");
        } else {
            System.out.println("Importación finalizada.");
        }
    }
}
