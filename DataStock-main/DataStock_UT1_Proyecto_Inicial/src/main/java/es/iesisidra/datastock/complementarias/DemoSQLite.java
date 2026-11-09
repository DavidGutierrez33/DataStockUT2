package es.iesisidra.datastock.complementarias;

import es.iesisidra.datastock.util.ColoresConsola;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Actividad comparativa (RA2-b): el mismo JDBC pero con SQLite, un gestor embebido.
 *
 * <p>Es un programa independiente: no está en los menús y DataStock sigue usando MySQL.
 * La base de datos es un simple fichero en datos/sqlite/productos_demo.db; no hace
 * falta usuario, contraseña ni servidor.</p>
 *
 * <p>Ejecución: {@code mvn exec:java -Dexec.mainClass=es.iesisidra.datastock.complementarias.DemoSQLite}</p>
 */
public final class DemoSQLite {

    private static final String CARPETA = "datos/sqlite";
    private static final String FICHERO = CARPETA + "/productos_demo.db";
    private static final String URL = "jdbc:sqlite:" + FICHERO;

    private DemoSQLite() { }

    public static void main(String[] args) {
        // SQLite crea el fichero .db al conectar, pero la carpeta tiene que existir antes
        try {
            Files.createDirectories(Paths.get(CARPETA));
        } catch (IOException e) {
            ColoresConsola.error("No se puede crear la carpeta " + CARPETA + ": " + e.getMessage());
            return;
        }
        Path ruta = Paths.get(FICHERO);
        boolean existiaAntes = Files.exists(ruta);

        String crearTabla = "CREATE TABLE IF NOT EXISTS producto_demo ("
                + "codigo TEXT PRIMARY KEY NOT NULL, "
                + "nombre TEXT NOT NULL)";
        // INSERT OR IGNORE: si el código ya existe no da error, simplemente no inserta (devuelve 0)
        String insertar = "INSERT OR IGNORE INTO producto_demo (codigo, nombre) VALUES (?, ?)";
        String consultar = "SELECT codigo, nombre FROM producto_demo ORDER BY codigo";

        String[][] datos = {
            {"SQL-001", "Producto demo A"},
            {"SQL-002", "Producto demo B"}
        };

        // Solo la URL: SQLite no tiene usuario ni contraseña
        try (Connection conexion = DriverManager.getConnection(URL)) {
            ColoresConsola.ok("Conexión SQLite abierta: " + FICHERO
                    + (existiaAntes ? " (el fichero ya existía)" : " (fichero nuevo)"));

            // Creamos la tabla con Statement porque el SQL es fijo
            try (Statement st = conexion.createStatement()) {
                st.executeUpdate(crearTabla);
                ColoresConsola.ok("Tabla producto_demo disponible.");
            }

            // Insertamos con PreparedStatement y contamos las filas que realmente entran
            int insertados = 0;
            int yaExistian = 0;
            try (PreparedStatement ps = conexion.prepareStatement(insertar)) {
                for (String[] fila : datos) {
                    ps.setString(1, fila[0]);
                    ps.setString(2, fila[1]);
                    if (ps.executeUpdate() == 1) {
                        insertados++;
                    } else {
                        yaExistian++;
                    }
                }
            }
            System.out.println("Insertados: " + insertados + " | Ya existentes: " + yaExistian);

            // Leemos la tabla
            try (Statement st = conexion.createStatement();
                 ResultSet rs = st.executeQuery(consultar)) {
                while (rs.next()) {
                    System.out.println(rs.getString("codigo") + " | " + rs.getString("nombre"));
                }
            }
        } catch (SQLException e) {
            ColoresConsola.error("Error SQLite: " + e.getMessage());
            return;
        }

        // Aquí la conexión ya está cerrada; el fichero sigue en disco para la siguiente ejecución
        try {
            ColoresConsola.ok("Conexión cerrada. El fichero " + FICHERO + " persiste ("
                    + Files.size(ruta) + " bytes).");
        } catch (IOException e) {
            ColoresConsola.aviso("Conexión cerrada, pero no se ha podido leer el tamaño del fichero.");
        }
    }
}
