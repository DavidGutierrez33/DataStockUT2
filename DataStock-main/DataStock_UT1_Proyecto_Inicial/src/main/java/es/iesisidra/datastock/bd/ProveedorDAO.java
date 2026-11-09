package es.iesisidra.datastock.bd;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentencias SQL de la tabla {@code proveedor}. El email es opcional:
 * cuando no se indica se guarda NULL con {@code setNull}.
 */
public final class ProveedorDAO {

    private ProveedorDAO() { }

    /** @return true si ya hay un proveedor con ese id. */
    public static boolean existeId(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM proveedor WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** @return true si OTRO proveedor (distinto de idExcluir) ya tiene ese email. */
    public static boolean existeEmail(String email, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM proveedor WHERE email = ? AND id <> ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** @return el proveedor con ese id, o null si no existe. */
    public static Proveedor buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre, email FROM proveedor WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // getString devuelve null si el email es NULL en la BD
                    return new Proveedor(rs.getInt("id"), rs.getString("nombre"), rs.getString("email"));
                }
                return null;
            }
        }
    }

    /** @return todos los proveedores ordenados por id. */
    public static List<Proveedor> listar() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, email FROM proveedor ORDER BY id";
        try (Connection con = ConexionBD.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Proveedor(rs.getInt("id"), rs.getString("nombre"), rs.getString("email")));
            }
        }
        return lista;
    }

    /** @return filas insertadas. */
    public static int insertar(Proveedor proveedor) throws SQLException {
        String sql = "INSERT INTO proveedor (id, nombre, email) VALUES (?, ?, ?)";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, proveedor.getId());
            ps.setString(2, proveedor.getNombre());
            asignarEmail(ps, 3, proveedor.getEmail());
            return ps.executeUpdate();
        }
    }

    /** Cambia nombre y email (el id no se modifica). @return filas modificadas. */
    public static int actualizar(Proveedor proveedor) throws SQLException {
        String sql = "UPDATE proveedor SET nombre = ?, email = ? WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, proveedor.getNombre());
            asignarEmail(ps, 2, proveedor.getEmail());
            ps.setInt(3, proveedor.getId());
            return ps.executeUpdate();
        }
    }

    /** Borra el proveedor; si tiene productos MySQL lo impide (error 1451). @return filas borradas. */
    public static int eliminar(int id) throws SQLException {
        String sql = "DELETE FROM proveedor WHERE id = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    /** Pone el email en el parámetro indicado, o NULL si no hay email. */
    private static void asignarEmail(PreparedStatement ps, int posicion, String email) throws SQLException {
        if (email == null) {
            ps.setNull(posicion, Types.VARCHAR);
        } else {
            ps.setString(posicion, email);
        }
    }
}
