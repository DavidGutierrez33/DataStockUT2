package es.iesisidra.datastock.bd;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.Producto;
import es.iesisidra.datastock.modelo.ProductoDetalle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentencias SQL de la tabla {@code producto}.
 *
 * <p>Las consultas usan JOIN con categoria y proveedor para devolver
 * objetos {@link ProductoDetalle} con los nombres incluidos.</p>
 *
 * <p>Ojo: aquí no hay ningún método que cambie solo el stock. El stock
 * se cambia únicamente con el procedimiento registrar_movimiento (MovimientoDAO).</p>
 */
public final class ProductoDAO {

    /** Parte común de todas las consultas de productos (sin WHERE ni ORDER BY). */
    private static final String SELECT_DETALLE =
            "SELECT p.codigo, p.nombre, p.precio, p.stock, p.categoria_id, p.proveedor_id, "
          + "c.nombre AS nombre_categoria, pr.nombre AS nombre_proveedor "
          + "FROM producto p "
          + "JOIN categoria c ON c.id = p.categoria_id "
          + "JOIN proveedor pr ON pr.id = p.proveedor_id ";

    private static final String INSERT =
            "INSERT INTO producto (codigo, nombre, precio, stock, categoria_id, proveedor_id) VALUES (?, ?, ?, ?, ?, ?)";

    private ProductoDAO() { }

    /** @return true si ya hay un producto con ese código. */
    public static boolean existeCodigo(String codigo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM producto WHERE codigo = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** @return número de productos guardados en MySQL. */
    public static int contar() throws SQLException {
        try (Connection con = ConexionBD.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM producto")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** @return el producto con ese código exacto, o null si no existe. */
    public static ProductoDetalle buscarPorCodigo(String codigo) throws SQLException {
        String sql = SELECT_DETALLE + "WHERE p.codigo = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return crearDesdeFila(rs);
                }
                return null;
            }
        }
    }

    /** @return todos los productos ordenados por código. */
    public static List<ProductoDetalle> listarTodos() throws SQLException {
        List<ProductoDetalle> lista = new ArrayList<>();
        String sql = SELECT_DETALLE + "ORDER BY p.codigo";
        try (Connection con = ConexionBD.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(crearDesdeFila(rs));
            }
        }
        return lista;
    }

    /**
     * Busca productos cuyo nombre contiene el texto. Los % se añaden al VALOR
     * del parámetro, no a la sentencia, así el texto del usuario nunca forma parte del SQL.
     */
    public static List<ProductoDetalle> buscarPorNombre(String fragmento) throws SQLException {
        List<ProductoDetalle> lista = new ArrayList<>();
        String sql = SELECT_DETALLE + "WHERE p.nombre LIKE ? ORDER BY p.codigo";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + fragmento + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(crearDesdeFila(rs));
                }
            }
        }
        return lista;
    }

    /** @return productos con stock estrictamente menor que el umbral, ordenados por stock y código. */
    public static List<ProductoDetalle> buscarStockInferior(int umbral) throws SQLException {
        List<ProductoDetalle> lista = new ArrayList<>();
        String sql = SELECT_DETALLE + "WHERE p.stock < ? ORDER BY p.stock, p.codigo";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, umbral);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(crearDesdeFila(rs));
                }
            }
        }
        return lista;
    }

    /** Alta de un producto (el stock inicial solo se indica aquí). @return filas insertadas. */
    public static int insertar(Producto p) throws SQLException {
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(INSERT)) {
            rellenarInsert(ps, p);
            return ps.executeUpdate();
        }
    }

    /**
     * Modifica nombre, precio, categoría y proveedor. El código y el stock NO se tocan.
     *
     * @return filas modificadas (0 si el código no existe)
     */
    public static int actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombre = ?, precio = ?, categoria_id = ?, proveedor_id = ? WHERE codigo = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setBigDecimal(2, p.getPrecio());
            ps.setInt(3, p.getCategoriaId());
            ps.setInt(4, p.getProveedorId());
            ps.setString(5, p.getCodigo());
            return ps.executeUpdate();
        }
    }

    /** Borra un producto; si tiene movimientos MySQL lo impide (error 1451). @return filas borradas. */
    public static int eliminar(String codigo) throws SQLException {
        String sql = "DELETE FROM producto WHERE codigo = ?";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            return ps.executeUpdate();
        }
    }

    /**
     * RF-JDBC-07: inserta un lote de productos (ya validados) en UNA transacción.
     * O se guardan todos (commit) o, si falla alguno por un error SQL, ninguno (rollback).
     *
     * @return número de productos insertados
     */
    public static int insertarLote(List<Producto> productos) throws SQLException {
        int insertados = 0;
        try (Connection con = ConexionBD.abrir()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(INSERT)) {
                // Reutilizamos el mismo PreparedStatement para todos los productos
                for (Producto p : productos) {
                    rellenarInsert(ps, p);
                    insertados += ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ignorada) {
                    // Conexión ya rota: se cierra igualmente al salir del try
                }
            }
        }
        return insertados;
    }

    /** Pasa los datos del producto a los 6 parámetros del INSERT. */
    private static void rellenarInsert(PreparedStatement ps, Producto p) throws SQLException {
        ps.setString(1, p.getCodigo());
        ps.setString(2, p.getNombre());
        ps.setBigDecimal(3, p.getPrecio()); // BigDecimal para el dinero, no double
        ps.setInt(4, p.getStock());
        ps.setInt(5, p.getCategoriaId());
        ps.setInt(6, p.getProveedorId());
    }

    /** Convierte la fila actual del ResultSet en un objeto ProductoDetalle. */
    private static ProductoDetalle crearDesdeFila(ResultSet rs) throws SQLException {
        return new ProductoDetalle(
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getBigDecimal("precio"),
                rs.getInt("stock"),
                rs.getInt("categoria_id"),
                rs.getInt("proveedor_id"),
                rs.getString("nombre_categoria"),
                rs.getString("nombre_proveedor"));
    }
}
