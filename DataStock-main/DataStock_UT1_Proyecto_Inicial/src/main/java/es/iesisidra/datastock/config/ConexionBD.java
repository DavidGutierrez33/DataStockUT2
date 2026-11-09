package es.iesisidra.datastock.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Abre conexiones JDBC con la base de datos MySQL {@code datastock}.
 *
 * <p>Los datos de conexión se leen del fichero {@code db.properties} (versión V2
 * de los apuntes). Ese fichero está en {@code src/main/resources}, NO se sube a Git
 * (está en .gitignore) y se crea copiando {@code db.properties.example}.</p>
 *
 * <p>Cada operación abre su propia conexión y la cierra con try-with-resources,
 * así no se queda ninguna conexión abierta mientras el usuario está en el menú.</p>
 */
public final class ConexionBD {

    private static final String FICHERO = "db.properties";

    private ConexionBD() { }

    /**
     * Abre una conexión nueva. Quien la pide es quien la tiene que cerrar.
     *
     * @return conexión abierta con MySQL
     * @throws SQLException si falta la configuración o no se puede conectar
     */
    public static Connection abrir() throws SQLException {
        Properties datos = new Properties();
        // Cargamos el fichero desde el classpath (Maven lo copia a target/classes)
        try (InputStream entrada = ConexionBD.class.getClassLoader().getResourceAsStream(FICHERO)) {
            if (entrada == null) {
                throw new SQLException("No se encuentra db.properties. Copia db.properties.example y complétalo.");
            }
            datos.load(entrada);
        } catch (IOException e) {
            throw new SQLException("Error leyendo db.properties", e);
        }

        String url = datos.getProperty("jdbc.url");
        String usuario = datos.getProperty("jdbc.user");
        String clave = datos.getProperty("jdbc.password");

        // Comprobamos que no falte ningún dato (la clave puede estar vacía en un MySQL local sin contraseña)
        if (url == null || url.isBlank() || usuario == null || usuario.isBlank() || clave == null) {
            throw new SQLException("Faltan datos en db.properties (jdbc.url, jdbc.user o jdbc.password).");
        }
        return DriverManager.getConnection(url.trim(), usuario.trim(), clave);
    }

    /**
     * Traduce una SQLException a un mensaje que entienda el usuario.
     * Nunca se muestra la contraseña ni la traza completa.
     *
     * <p>Los números son los códigos de error de MySQL (getErrorCode) y el SQLState
     * que empieza por "08" indica problemas de conexión.</p>
     */
    public static String explicarError(SQLException e) {
        int codigo = e.getErrorCode();
        String estado = e.getSQLState();

        // Errores que lanzamos nosotros mismos (sin SQLState): su mensaje ya es claro
        if (estado == null) {
            return e.getMessage();
        }
        if (estado.startsWith("08")) {
            return "No se puede conectar con MySQL. Comprueba que el servidor está arrancado y la URL de db.properties.";
        }
        switch (codigo) {
            case 1045:
                return "Usuario o contraseña de MySQL incorrectos. Revisa db.properties.";
            case 1049:
                return "La base de datos de la URL no existe. Ejecuta el script DataStock_BD_MySQL.sql.";
            case 1146:
                return "Falta una tabla del esquema datastock. Ejecuta el script DataStock_BD_MySQL.sql.";
            case 1062:
                return "Ya existe un registro con ese valor (clave duplicada).";
            case 1451:
                return "No se puede borrar porque hay otros registros que dependen de él (clave foránea).";
            case 1452:
                return "La categoría o el proveedor indicado no existe (clave foránea).";
            case 3819:
                return "Los datos no cumplen una restricción CHECK de la tabla.";
            case 1644:
                // SIGNAL SQLSTATE '45000' del procedimiento registrar_movimiento
                return e.getMessage();
            default:
                return "Error de base de datos: " + e.getMessage() + " (SQLState " + estado + ", código " + codigo + ")";
        }
    }
}
