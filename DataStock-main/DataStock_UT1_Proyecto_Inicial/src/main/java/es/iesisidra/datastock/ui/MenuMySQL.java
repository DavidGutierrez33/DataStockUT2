package es.iesisidra.datastock.ui;

import es.iesisidra.datastock.bd.CategoriaDAO;
import es.iesisidra.datastock.bd.ProveedorDAO;
import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Categoria;
import es.iesisidra.datastock.modelo.Proveedor;
import es.iesisidra.datastock.servicio.Inventario;
import es.iesisidra.datastock.util.ColoresConsola;
import es.iesisidra.datastock.util.LectorConsola;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Opción 7 del menú principal: gestión de la base de datos MySQL (UT2).
 *
 * <p>Esta clase solo pide datos y muestra resultados; el SQL está en las clases
 * del paquete {@code bd}. Aquí están las opciones 7.1 (conexión), 7.2 (categorías),
 * 7.3 (proveedores) y 7.9 (demo de transacción). Las de productos, consultas,
 * movimientos, historial e importación están en {@link MenuProductosMySQL}.</p>
 *
 * <p>Cada opción captura su SQLException, así un fallo nunca cierra la aplicación
 * y se vuelve al menú.</p>
 */
public class MenuMySQL {

    /** Tablas del script DataStock_BD_MySQL.sql (constantes, no datos del usuario). */
    private static final String[] TABLAS = {"categoria", "proveedor", "producto", "movimiento_stock"};
    private static final int MAX_NOMBRE_CATEGORIA = 60;
    private static final int MAX_NOMBRE_PROVEEDOR = 100;
    private static final int MAX_EMAIL = 120;
    /** Formato básico de email: algo@algo.algo sin espacios. */
    private static final Pattern PATRON_EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final MenuProductosMySQL menuProductos;

    /** @param inventario inventario en memoria de la UT1 (solo se usa en la opción 7.8). */
    public MenuMySQL(Inventario inventario) {
        this.menuProductos = new MenuProductosMySQL(inventario);
    }

    /** Muestra el menú MySQL hasta que se elige volver. */
    public void iniciar() {
        int opcion;
        do {
            System.out.println(ColoresConsola.pintar("\n================================================", ColoresConsola.CIAN));
            System.out.println(ColoresConsola.pintar("              GESTIÓN MYSQL - UT2", ColoresConsola.NEGRITA));
            System.out.println(ColoresConsola.pintar("================================================", ColoresConsola.CIAN));
            System.out.println("1. Comprobar conexión y mostrar información de la BD");
            System.out.println("2. Gestionar categorías");
            System.out.println("3. Gestionar proveedores");
            System.out.println("4. Gestionar productos (CRUD)");
            System.out.println("5. Consultar y buscar productos");
            System.out.println("6. Registrar entrada o salida de stock");
            System.out.println("7. Consultar historial de movimientos");
            System.out.println("8. Importar productos de memoria a MySQL");
            System.out.println("9. Demostración de transacción con commit/rollback");
            System.out.println("0. Volver al menú principal");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 9);
            switch (opcion) {
                case 1 -> comprobarConexion();
                case 2 -> menuCategorias();
                case 3 -> menuProveedores();
                case 4 -> menuProductos.menuProductos();
                case 5 -> menuProductos.menuConsultas();
                case 6 -> menuProductos.menuMovimientos();
                case 7 -> menuProductos.menuHistorial();
                case 8 -> menuProductos.importarDesdeMemoria();
                case 9 -> demoTransaccion();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    // ------------------------------------------------------------------
    // 7.1 · RF-JDBC-01 · Conexión
    // ------------------------------------------------------------------

    private void comprobarConexion() {
        titulo("COMPROBAR CONEXIÓN");
        // La conexión, el Statement y cada ResultSet se cierran solos al salir del try
        try (Connection con = ConexionBD.abrir()) {
            ColoresConsola.ok("Conexión establecida correctamente.");
            DatabaseMetaData meta = con.getMetaData();
            System.out.println("Base de datos: " + con.getCatalog());
            System.out.println("Gestor: " + meta.getDatabaseProductName());
            System.out.println("Versión: " + meta.getDatabaseProductVersion());
            System.out.println("Driver: " + meta.getDriverName() + " " + meta.getDriverVersion());

            // Una consulta sencilla a cada tabla para comprobar que el esquema está completo
            try (Statement st = con.createStatement()) {
                for (String tabla : TABLAS) {
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
                        rs.next();
                        System.out.printf("  Registros en %-17s %d%n", tabla + ":", rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
        LectorConsola.pausar();
    }

    // ------------------------------------------------------------------
    // 7.2 · RF-JDBC-02 · Categorías
    // ------------------------------------------------------------------

    private void menuCategorias() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE CATEGORÍAS ----------");
            System.out.println("1. Dar de alta una categoría");
            System.out.println("2. Listar categorías");
            System.out.println("3. Modificar el nombre de una categoría");
            System.out.println("4. Eliminar una categoría");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            switch (opcion) {
                case 1 -> altaCategoria();
                case 2 -> listarCategorias();
                case 3 -> modificarCategoria();
                case 4 -> eliminarCategoria();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void altaCategoria() {
        titulo("ALTA DE CATEGORÍA");
        try {
            int id = pedirIdPositivo("ID de categoría (Intro para cancelar): ");
            if (id == 0) {
                cancelada();
                return;
            }
            if (CategoriaDAO.existeId(id)) {
                ColoresConsola.error("Ya existe una categoría con ID " + id + ".");
                return;
            }
            String nombre = pedirTexto("Nombre (Intro para cancelar): ", MAX_NOMBRE_CATEGORIA);
            if (nombre == null) {
                cancelada();
                return;
            }
            if (CategoriaDAO.existeNombre(nombre, 0)) {
                ColoresConsola.error("Ya existe una categoría llamada \"" + nombre + "\".");
                return;
            }
            int filas = CategoriaDAO.insertar(new Categoria(id, nombre));
            if (filas == 1) {
                ColoresConsola.ok("Categoría registrada correctamente.");
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void listarCategorias() {
        titulo("LISTADO DE CATEGORÍAS");
        try {
            List<Categoria> categorias = CategoriaDAO.listar();
            if (categorias.isEmpty()) {
                ColoresConsola.aviso("No hay categorías registradas.");
                return;
            }
            System.out.printf("%-6s %s%n", "ID", "NOMBRE");
            System.out.println("----------------------------------------");
            for (Categoria c : categorias) {
                System.out.printf("%-6d %s%n", c.getId(), c.getNombre());
            }
            System.out.println("Total: " + categorias.size());
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void modificarCategoria() {
        titulo("MODIFICAR CATEGORÍA");
        try {
            int id = pedirIdPositivo("ID de la categoría (Intro para cancelar): ");
            if (id == 0) {
                cancelada();
                return;
            }
            Categoria actual = CategoriaDAO.buscarPorId(id);
            if (actual == null) {
                ColoresConsola.error("No existe ninguna categoría con ID " + id + ".");
                return;
            }
            System.out.println("Nombre actual: " + actual.getNombre());
            String nuevoNombre = pedirTexto("Nuevo nombre (Intro para cancelar): ", MAX_NOMBRE_CATEGORIA);
            if (nuevoNombre == null) {
                cancelada();
                return;
            }
            // Excluimos su propio id: poner el mismo nombre que ya tenía no es un duplicado
            if (CategoriaDAO.existeNombre(nuevoNombre, id)) {
                ColoresConsola.error("Ya existe otra categoría llamada \"" + nuevoNombre + "\".");
                return;
            }
            if (!LectorConsola.confirmar("[AVISO] ¿Cambiar \"" + actual.getNombre() + "\" por \"" + nuevoNombre + "\"?")) {
                cancelada();
                return;
            }
            int filas = CategoriaDAO.actualizarNombre(id, nuevoNombre);
            if (filas == 1) {
                ColoresConsola.ok("Categoría " + id + " actualizada.");
            } else {
                ColoresConsola.error("No se ha modificado ninguna fila: la categoría " + id + " ya no existe.");
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void eliminarCategoria() {
        titulo("ELIMINAR CATEGORÍA");
        try {
            int id = pedirIdPositivo("ID de la categoría (Intro para cancelar): ");
            if (id == 0) {
                cancelada();
                return;
            }
            Categoria actual = CategoriaDAO.buscarPorId(id);
            if (actual == null) {
                ColoresConsola.error("No existe ninguna categoría con ID " + id + ".");
                return;
            }
            System.out.println("ID: " + actual.getId() + " | Nombre: " + actual.getNombre());
            if (!LectorConsola.confirmar("[AVISO] ¿Confirmas la eliminación?")) {
                cancelada();
                return;
            }
            int filas = CategoriaDAO.eliminar(id);
            if (filas == 1) {
                ColoresConsola.ok("Categoría " + id + " eliminada.");
            } else {
                ColoresConsola.error("No se ha borrado nada: la categoría " + id + " ya no existía.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                // La FK de producto lo impide: no tocamos los productos
                ColoresConsola.error("No se puede eliminar la categoría: tiene productos asociados. No se ha borrado nada.");
            } else {
                ColoresConsola.error(ConexionBD.explicarError(e));
            }
        }
    }

    // ------------------------------------------------------------------
    // 7.3 · RF-JDBC-02 · Proveedores
    // ------------------------------------------------------------------

    private void menuProveedores() {
        int opcion;
        do {
            System.out.println("\n---------- MENÚ DE PROVEEDORES ----------");
            System.out.println("1. Dar de alta un proveedor");
            System.out.println("2. Listar proveedores");
            System.out.println("3. Modificar un proveedor");
            System.out.println("4. Eliminar un proveedor");
            System.out.println("0. Volver al menú MySQL");
            opcion = LectorConsola.leerOpcion("Selecciona una opción: ", 0, 4);
            switch (opcion) {
                case 1 -> altaProveedor();
                case 2 -> listarProveedores();
                case 3 -> modificarProveedor();
                case 4 -> eliminarProveedor();
                case 0 -> { }
                default -> throw new IllegalStateException("Opción inesperada");
            }
        } while (opcion != 0);
    }

    private void altaProveedor() {
        titulo("ALTA DE PROVEEDOR");
        try {
            int id = pedirIdPositivo("ID de proveedor (Intro para cancelar): ");
            if (id == 0) {
                cancelada();
                return;
            }
            if (ProveedorDAO.existeId(id)) {
                ColoresConsola.error("Ya existe un proveedor con ID " + id + ".");
                return;
            }
            String nombre = pedirTexto("Nombre (Intro para cancelar): ", MAX_NOMBRE_PROVEEDOR);
            if (nombre == null) {
                cancelada();
                return;
            }
            String email = pedirEmail("Email (opcional, Intro para dejarlo vacío): ", id);
            int filas = ProveedorDAO.insertar(new Proveedor(id, nombre, email));
            if (filas == 1) {
                ColoresConsola.ok("Proveedor registrado correctamente.");
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void listarProveedores() {
        titulo("LISTADO DE PROVEEDORES");
        try {
            List<Proveedor> proveedores = ProveedorDAO.listar();
            if (proveedores.isEmpty()) {
                ColoresConsola.aviso("No hay proveedores registrados.");
                return;
            }
            System.out.printf("%-6s %-35s %s%n", "ID", "NOMBRE", "EMAIL");
            System.out.println("--------------------------------------------------------------------");
            for (Proveedor p : proveedores) {
                String email = (p.getEmail() == null) ? "—" : p.getEmail();
                System.out.printf("%-6d %-35s %s%n", p.getId(), p.getNombre(), email);
            }
            System.out.println("Total: " + proveedores.size());
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void modificarProveedor() {
        titulo("MODIFICAR PROVEEDOR");
        try {
            int id = pedirIdPositivo("ID del proveedor (Intro para cancelar): ");
            if (id == 0) {
                cancelada();
                return;
            }
            Proveedor actual = ProveedorDAO.buscarPorId(id);
            if (actual == null) {
                ColoresConsola.error("No existe ningún proveedor con ID " + id + ".");
                return;
            }
            String emailActual = (actual.getEmail() == null) ? "—" : actual.getEmail();
            System.out.println("Nombre actual: " + actual.getNombre());
            System.out.println("Email actual:  " + emailActual);
            System.out.println("Pulsa Intro para conservar el dato actual. En el email, escribe - para dejarlo sin valor.");

            // Nombre: Intro conserva el actual
            String nombre = pedirTexto("Nuevo nombre: ", MAX_NOMBRE_PROVEEDOR);
            if (nombre == null) {
                nombre = actual.getNombre();
            }

            // Email: Intro conserva, "-" lo deja a NULL, otro texto se valida
            String email = actual.getEmail();
            while (true) {
                String texto = LectorConsola.leerTexto("Nuevo email: ").trim();
                if (texto.isEmpty()) {
                    break;
                }
                if (texto.equals("-")) {
                    email = null;
                    break;
                }
                if (emailValido(texto, id)) {
                    email = texto;
                    break;
                }
            }

            if (!LectorConsola.confirmar("[AVISO] ¿Guardar nombre \"" + nombre + "\" y email \""
                    + (email == null ? "—" : email) + "\"?")) {
                cancelada();
                return;
            }
            int filas = ProveedorDAO.actualizar(new Proveedor(id, nombre, email));
            if (filas == 1) {
                ColoresConsola.ok("Proveedor " + id + " actualizado.");
            } else {
                ColoresConsola.error("No se ha modificado ninguna fila: el proveedor " + id + " ya no existe.");
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
    }

    private void eliminarProveedor() {
        titulo("ELIMINAR PROVEEDOR");
        try {
            int id = pedirIdPositivo("ID del proveedor (Intro para cancelar): ");
            if (id == 0) {
                cancelada();
                return;
            }
            Proveedor actual = ProveedorDAO.buscarPorId(id);
            if (actual == null) {
                ColoresConsola.error("No existe ningún proveedor con ID " + id + ".");
                return;
            }
            System.out.println("ID: " + actual.getId() + " | Nombre: " + actual.getNombre()
                    + " | Email: " + (actual.getEmail() == null ? "—" : actual.getEmail()));
            if (!LectorConsola.confirmar("[AVISO] ¿Confirmas la eliminación?")) {
                cancelada();
                return;
            }
            int filas = ProveedorDAO.eliminar(id);
            if (filas == 1) {
                ColoresConsola.ok("Proveedor " + id + " eliminado.");
            } else {
                ColoresConsola.error("No se ha borrado nada: el proveedor " + id + " ya no existía.");
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1451) {
                ColoresConsola.error("No se puede eliminar el proveedor: tiene productos asociados (integridad referencial). No se ha borrado nada.");
            } else {
                ColoresConsola.error(ConexionBD.explicarError(e));
            }
        }
    }

    /**
     * Pide un email opcional. Devuelve null si se deja vacío.
     * Repite la pregunta mientras el formato no sea válido o el email ya lo tenga otro proveedor.
     */
    private String pedirEmail(String mensaje, int idProveedor) throws SQLException {
        while (true) {
            String texto = LectorConsola.leerTexto(mensaje).trim();
            if (texto.isEmpty()) {
                return null;
            }
            if (emailValido(texto, idProveedor)) {
                return texto;
            }
        }
    }

    /** Comprueba longitud, formato básico y que no lo use otro proveedor. Muestra el motivo si falla. */
    private boolean emailValido(String email, int idProveedor) throws SQLException {
        if (email.length() > MAX_EMAIL) {
            ColoresConsola.error("El email no puede superar " + MAX_EMAIL + " caracteres.");
            return false;
        }
        if (!PATRON_EMAIL.matcher(email).matches()) {
            ColoresConsola.error("El email no tiene un formato válido (ejemplo: ventas@empresa.com).");
            return false;
        }
        if (ProveedorDAO.existeEmail(email, idProveedor)) {
            ColoresConsola.error("Ese email ya está registrado en otro proveedor.");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // 7.9 · RF-JDBC-08 · Demostración de commit y rollback
    // ------------------------------------------------------------------

    private void demoTransaccion() {
        titulo("DEMOSTRACIÓN DE TRANSACCIONES JDBC");
        try {
            // Usamos ids por encima del mayor que existe: así seguro que están libres
            // y no pueden tener productos asociados.
            int base = CategoriaDAO.maximoId();
            int idA1 = base + 1;
            int idA2 = base + 2;
            int idB = base + 3;

            System.out.println("Esta demostración solo usa categorías de prueba nuevas:");
            System.out.println("  Escenario A (commit):   inserta las categorías " + idA1 + " y " + idA2 + " y las confirma.");
            System.out.println("  Escenario B (rollback): inserta la categoría " + idB
                    + " y luego la repite para provocar un error de clave duplicada.");
            if (!LectorConsola.confirmar("[AVISO] ¿Empezar la demostración?")) {
                cancelada();
                return;
            }

            // ----- Escenario A: dos inserciones correctas y commit -----
            System.out.println("\n--- Escenario A: commit ---");
            List<Categoria> loteA = new ArrayList<>();
            loteA.add(new Categoria(idA1, "Demo transacción " + idA1));
            loteA.add(new Categoria(idA2, "Demo transacción " + idA2));
            CategoriaDAO.insertarEnTransaccion(loteA);
            // Comprobamos con un SELECT que de verdad están en MySQL
            if (CategoriaDAO.existeId(idA1) && CategoriaDAO.existeId(idA2)) {
                ColoresConsola.ok("Commit realizado: las categorías " + idA1 + " y " + idA2 + " existen en MySQL.");
            } else {
                ColoresConsola.error("Algo ha fallado: no se encuentran las categorías del escenario A.");
            }

            // ----- Escenario B: segunda inserción duplicada y rollback -----
            System.out.println("\n--- Escenario B: rollback ---");
            List<Categoria> loteB = new ArrayList<>();
            loteB.add(new Categoria(idB, "Demo transacción " + idB));
            loteB.add(new Categoria(idB, "Demo transacción " + idB + " bis")); // mismo id: error 1062
            try {
                CategoriaDAO.insertarEnTransaccion(loteB);
                ColoresConsola.error("No se ha producido el error esperado.");
            } catch (SQLException esperado) {
                ColoresConsola.aviso("Error esperado en la segunda inserción: " + ConexionBD.explicarError(esperado));
                if (!CategoriaDAO.existeId(idB)) {
                    ColoresConsola.ok("Rollback correcto: la primera inserción (categoría " + idB + ") tampoco se ha guardado.");
                } else {
                    ColoresConsola.error("La categoría " + idB + " sigue en MySQL: el rollback no ha funcionado.");
                }
            }

            // ----- Limpieza -----
            System.out.println("\nIDs usados: " + idA1 + ", " + idA2 + " (confirmados) y " + idB + " (deshecho).");
            System.out.println("Para borrarlos a mano en MySQL Workbench:");
            System.out.println("  DELETE FROM categoria WHERE id IN (" + idA1 + ", " + idA2 + ");");
            if (LectorConsola.confirmar("[AVISO] ¿Quieres borrar ahora las categorías de demostración "
                    + idA1 + " y " + idA2 + "?")) {
                CategoriaDAO.eliminar(idA1);
                CategoriaDAO.eliminar(idA2);
                ColoresConsola.ok("Categorías de demostración eliminadas.");
            } else {
                ColoresConsola.aviso("Se conservan para que puedas comprobarlas en MySQL.");
            }
        } catch (SQLException e) {
            ColoresConsola.error(ConexionBD.explicarError(e));
        }
        LectorConsola.pausar();
    }

    // ------------------------------------------------------------------
    // Ayudas para pedir datos (también las usa MenuProductosMySQL)
    // ------------------------------------------------------------------

    /**
     * Pide un entero positivo. Repite si no es válido.
     *
     * @return el número, o 0 si el usuario pulsa Intro sin escribir (cancelar)
     */
    static int pedirIdPositivo(String mensaje) {
        while (true) {
            String texto = LectorConsola.leerTexto(mensaje).trim();
            if (texto.isEmpty()) {
                return 0;
            }
            try {
                int numero = Integer.parseInt(texto);
                if (numero > 0) {
                    return numero;
                }
            } catch (NumberFormatException e) {
                // Lo tratamos igual que un número no positivo
            }
            ColoresConsola.error("Debe ser un número entero positivo.");
        }
    }

    /**
     * Pide un texto obligatorio de 1 a maximo caracteres, sin espacios al principio ni al final.
     *
     * @return el texto, o null si se deja vacío
     */
    static String pedirTexto(String mensaje, int maximo) {
        while (true) {
            String texto = LectorConsola.leerTexto(mensaje).trim();
            if (texto.isEmpty()) {
                return null;
            }
            if (texto.length() <= maximo) {
                return texto;
            }
            ColoresConsola.error("Máximo " + maximo + " caracteres (has escrito " + texto.length() + ").");
        }
    }

    static void titulo(String texto) {
        System.out.println(ColoresConsola.pintar("\n" + texto, ColoresConsola.NEGRITA));
        System.out.println("----------------------------------------");
    }

    static void cancelada() {
        ColoresConsola.aviso("Operación cancelada. No se ha modificado nada.");
    }
}

// Profe, he aplicado lo visto en el Tema 2 de JDBC: try-with-resources para cerrar
// Connection, Statement y ResultSet, PreparedStatement en todo lo que escribe el usuario
// y commit/rollback con una sola conexión. Espero que esté todo correcto.
