package es.iesisidra.datastock.bd;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentencias SQL de la tabla {@code categoria}.
 *
 * <p>Aquí solo hay JDBC: no se pide nada por teclado ni se muestran menús.
 * Los métodos lanzan SQLException y es el menú quien decide qué mensaje enseñar.</p>
 */
public final class CategoriaDAO {

    private CategoriaDAO() { }

    /** @return true si ya hay una categoría con ese id. */
    public static boolean existeId(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categoria WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); // COUNT(*) siempre devuelve una fila
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Comprueba si el nombre lo usa OTRA categoría (distinta de idExcluir).
     * Al dar de alta se pasa 0 como idExcluir. La colación de la BD no distingue
     * mayúsculas, así que "monitores" y "Monitores" cuentan como repetidos.
     */
    public static boolean existeNombre(String nombre, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categoria WHERE nombre = ? AND id <> ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** @return la categoría con ese id, o null si no existe. */
    public static Categoria buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre FROM categoria WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Categoria(rs.getInt("id"), rs.getString("nombre"));
                }
                return null;
            }
        }
    }

    /** @return todas las categorías ordenadas por id (lista vacía si no hay). */
    public static List<Categoria> listar() throws SQLException {
        List<Categoria> lista = new ArrayList<>();
        // SQL fijo, sin datos del usuario: vale con Statement
        String sql = "SELECT id, nombre FROM categoria ORDER BY id";
        try (Connection con = ConexionBD.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        }
        return lista;
    }

    /** @return filas insertadas (1 si ha ido bien). */
    public static int insertar(Categoria categoria) throws SQLException {
        String sql = "INSERT INTO categoria (id, nombre) VALUES (?, ?)";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, categoria.getId());
            ps.setString(2, categoria.getNombre());
            return ps.executeUpdate();
        }
    }

    /** @return filas modificadas (0 si el id no existía). El id nunca se cambia. */
    public static int actualizarNombre(int id, String nuevoNombre) throws SQLException {
        String sql = "UPDATE categoria SET nombre = ? WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoNombre);
            ps.setInt(2, id);
            return ps.executeUpdate();
        }
    }

    /**
     * Borra la categoría. Si tiene productos, MySQL lanza el error 1451 (clave foránea)
     * y no se borra nada: no borramos los productos para forzarlo.
     *
     * @return filas borradas (0 si no existía)
     */
    public static int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM categoria WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /** @return el id más alto usado (0 si la tabla está vacía). Sirve para elegir ids libres en la demo. */
    public static int maximoId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM categoria";
        try (Connection con = ConexionBD.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * RF-JDBC-08: inserta varias categorías en UNA transacción.
     * Si todas van bien se hace commit; si falla alguna se hace rollback de todas
     * y se vuelve a lanzar la excepción para que el menú la muestre.
     */
    public static void insertarEnTransaccion(List<Categoria> categorias) throws SQLException {
        String sql = "INSERT INTO categoria (id, nombre) VALUES (?, ?)";
        try (Connection con = ConexionBD.abrir()) {
            // Desactivamos el autocommit: a partir de aquí nada se confirma solo
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (Categoria c : categorias) {
                    ps.setInt(1, c.getId());
                    ps.setString(2, c.getNombre());
                    ps.executeUpdate();
                }
                con.commit(); // Todas bien: confirmamos
            } catch (SQLException e) {
                // Alguna ha fallado: deshacemos también las que sí se habían insertado
                try {
                    con.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            } finally {
                // Dejamos la conexión como estaba antes de cerrarla
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ignorada) {
                    // Si la conexión ya está rota no podemos hacer nada más
                }
            }
        }
    }
}
