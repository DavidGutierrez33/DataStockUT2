package es.iesisidra.datastock.ui;

import java.io.File;

import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;
/**
 * Presentación de menús y navegación de DataStock.
 * <p>Las opciones 1-6 (UT1) trabajan con el inventario en memoria; la opción 7 (UT2)
 * trabaja con MySQL. Los dos almacenes no se sincronizan solos: solo la opción 7.8
 * copia productos de memoria a MySQL.</p>
 * <p>Los métodos marcados TODO son puntos de partida, NO soluciones de los
 * requisitos. El alumnado deberá delegar el acceso a ficheros, la validación
 * y el procesamiento en métodos o clases independientes.</p>
 */
public class MenuPrincipal {
    private final Inventario inventario;
    /** Operaciones que ejecuta cada opción del menú (el switch solo delega en ellas). */
    private final OperacionesInventario operaciones;
    /** UT2: menú de la opción 7 (MySQL). */
    private final MenuMySQL menuMySQL;

    /** @param inventario colección común de productos de esta ejecución. */
    public MenuPrincipal(Inventario inventario) {
        this.inventario = inventario;
        this.operaciones = new OperacionesInventario(inventario);
        this.menuMySQL = new MenuMySQL(inventario);
    }

    /** Ejecuta el menú principal hasta que el usuario selecciona salir. */
    public void iniciar() {
        int opcion;
        do {
            mostrarCabecera();
            System.out.println("1. Explorar ficheros y directorios (UT1)");
            System.out.println("2. Importar productos desde JSON (UT1)");
            System.out.println("3. Importar productos desde XML (UT1)");
            System.out.println("4. Consultar productos en memoria (UT1)");
            System.out.println("5. Exportar productos (UT1)");
            System.out.println("6. Guardar y recuperar inventario (UT1)");
            System.out.println("7. Gestionar base de datos MySQL (UT2)");
            System.out.println("0. Salir");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 7);
            switch (opcion) {
                case 1 -> explorar();
                case 2 -> Inventario.gestionarImportacionJson();
                case 3 -> operaciones.importarXml();
                case 4 -> menuConsultas();
                case 5 -> menuExportaciones();
                case 6 -> menuSerializacion();
                case 7 -> menuMySQL.iniciar();
                case 0 -> System.out.println(ColoresConsola.pintar("Hasta pronto.", ColoresConsola.VERDE));
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void mostrarCabecera() {
        System.out.println(ColoresConsola.pintar("\n================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("         DATASTOCK - GESTIÓN DE INVENTARIO", ColoresConsola.NEGRITA));
        System.out.println(ColoresConsola.pintar("                 FASE 2 - UT2", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("================================================", ColoresConsola.CIAN));
        System.out.println(ColoresConsola.pintar("Productos en memoria: " + inventario.getProductos().size(), ColoresConsola.GRIS));
    }

    private void menuConsultas() {
        int opcion;
        do {
            System.out.println("\n---------- CONSULTA DE PRODUCTOS ----------");
            System.out.println("1. Listar todos los productos");
            System.out.println("2. Buscar producto por código");
            System.out.println("3. Buscar productos por nombre");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 3);
            switch (opcion) {
                case 1 -> operaciones.listar();
                case 2 -> operaciones.buscarPorCodigo();
                case 3 -> operaciones.buscarPorNombre();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void menuExportaciones() {
        int opcion;
        do {
            System.out.println("\n---------- EXPORTAR PRODUCTOS ----------");
            System.out.println("1. Exportar productos a JSON");
            System.out.println("2. Exportar productos a XML");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> operaciones.exportarJson();
                case 2 -> operaciones.exportarXml();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void menuSerializacion() {
        int opcion;
        do {
            System.out.println("\n---------- GUARDAR Y RECUPERAR INVENTARIO ----------");
            System.out.println("1. Guardar inventario en fichero .dat");
            System.out.println("2. Recuperar inventario desde fichero .dat");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 2);
            switch (opcion) {
                case 1 -> operaciones.guardarDat();
                case 2 -> operaciones.recuperarDat();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }
    private void explorar(){
        System.out.println(ColoresConsola.AZUL+"---CONSULTA DE SISTEMA DE ARCHIVOS---" +ColoresConsola.RESET);

        //Leemos con el lector de texto y usamos el trim para que los espacios que hay no perjudiquen en la busqueda y pueda dar un resultado acertado
        String entradaRuta = LectorConsola.leerTexto("Introduce la ruta: ").trim();

        //obtenemos la ruta de entrada anterior y lo guardamos como fichero
        File archivoRuta = new File(entradaRuta);

        //Comporbamos que la ruta exista para si existe pueda realizar el programa 
        if(!archivoRuta.exists()){
            System.out.println(ColoresConsola.ROJO+"ERROR: La ruta indicada no existe!!" + ColoresConsola.RESET);
            LectorConsola.pausar();
        }
        //sabiendo que la ruta existe comienza a comprobar que es si es archivo o direct
        else{
            
            //Comprueba si es un fichero para asi poder pedir el tamaño y el nombre 
            if(archivoRuta.isFile()){
                System.out.println(ColoresConsola.VERDE+"Este es un fichero" +ColoresConsola.RESET);
                System.out.println("Tamaño:" +archivoRuta.length());
            }
            //Comprueba si es un directorio y nos dara la inforamción de todo lo que haya dentro de el
            else if(archivoRuta.isDirectory()){
                System.out.println(ColoresConsola.VERDE+"Tu archivo es un directorio vamos a revisar que hay dentro"+ColoresConsola.RESET);

                //con este podremos guardar todos los archivos que vaya leyendo en un array
                File[] contenidoDirectorio = archivoRuta.listFiles();

                //Este if comprueba si tiene permisos de acceso al directorio, si no los tiene lanza el error por pantalla
                if(contenidoDirectorio==null){
                    System.out.println(ColoresConsola.ROJO+"No tengo permisos para abrir el directorio"+ColoresConsola.RESET);
                }
                //este if comprueba la carpeta, si no tiene documentos te dira que no hay nada la carpeta
                else if(contenidoDirectorio.length==0){
                    System.out.println(ColoresConsola.AMARILLO+"La carpeta esta vacia"+ColoresConsola.RESET);
                    LectorConsola.pausar();
                //Por ultimo aqui te ira comprobando todos los archivos mediante un for y te dira si es un archivo o directorio
                }else{
                    for (File archivos:contenidoDirectorio){
                        //lee si esa un directorio y si lo es te lo dice
                        if (archivos.isDirectory()){
                            System.out.println(ColoresConsola.CIAN+archivos + " Es un directorio"+ColoresConsola.RESET);
                        }
                        //lee si es un archivo y te lo dice
                        else if (archivos.isFile()){
                            System.out.println(ColoresConsola.VERDE+archivos+" Es un archivo"+ColoresConsola.RESET);
                        }
                        //si no es ni directorio ni archivo
                        else{
                            System.out.println(ColoresConsola.ROJO+archivos+" No es ni fichero ni directorio"+ColoresConsola.RESET);
                        }
                    }
                }

            }else{
                System.out.println(ColoresConsola.ROJO+"ERROR no es ni directorio ni fichero porfavor intentelo de nuevo"+ColoresConsola.RESET);
            }
        }

    }

    /** Sustituir las llamadas por servicios propios a medida que se implementen los RF. */
    private void pendiente(String requisito) {
        System.out.println(ColoresConsola.pintar("[PENDIENTE] " + requisito, ColoresConsola.AMARILLO));
        System.out.println("Esta funcionalidad debe desarrollarla el alumnado.");
        LectorConsola.pausar();
    }
}

