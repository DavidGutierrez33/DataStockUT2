package es.iesisidra.datastock.bd;

import es.iesisidra.datastock.config.ConexionBD;
import es.iesisidra.datastock.modelo.MovimientoStock;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Movimientos de stock: el registro se hace llamando al procedimiento almacenado
 * {@code registrar_movimiento} y las consultas leen la tabla {@code movimiento_stock}.
 */
public final class MovimientoDAO {

    private static final String SELECT_MOVIMIENTOS =
            "SELECT id, producto_codigo, tipo, cantidad, fecha, stock_anterior, stock_posterior "
          + "FROM movimiento_stock ";

    private MovimientoDAO() { }

    /**
     * Llama a registrar_movimiento(codigo, tipo, cantidad) con CallableStatement.
     *
     * <p>No abrimos una transacción en Java: el procedimiento ya hace START TRANSACTION,
     * SELECT ... FOR UPDATE, UPDATE, INSERT y COMMIT (o ROLLBACK si falla). Si el stock
     * no llega, el procedimiento hace SIGNAL y aquí nos llega como SQLException.</p>
     *
     * @param tipo "ENTRADA" o "SALIDA"
     */
    public static void registrar(String codigo, String tipo, int cantidad) throws SQLException {
        try (Connection con = ConexionBD.abrir();
             CallableStatement cs = con.prepareCall("{call registrar_movimiento(?, ?, ?)}")) {
            cs.setString(1, codigo);
            cs.setString(2, tipo);
            cs.setInt(3, cantidad);
            cs.execute();
        }
    }

    /** @return el último movimiento de ese producto, o null si no tiene ninguno. */
    public static MovimientoStock ultimoDeProducto(String codigo) throws SQLException {
        String sql = SELECT_MOVIMIENTOS + "WHERE producto_codigo = ? ORDER BY id DESC LIMIT 1";
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

    /** @return todo el historial, ordenado por fecha y después por id. */
    public static List<MovimientoStock> listarTodos() throws SQLException {
        List<MovimientoStock> lista = new ArrayList<>();
        String sql = SELECT_MOVIMIENTOS + "ORDER BY fecha, id";
        try (Connection con = ConexionBD.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(crearDesdeFila(rs));
            }
        }
        return lista;
    }

    /** @return historial de un producto, mismo orden que el completo. */
    public static List<MovimientoStock> listarPorProducto(String codigo) throws SQLException {
        List<MovimientoStock> lista = new ArrayList<>();
        String sql = SELECT_MOVIMIENTOS + "WHERE producto_codigo = ? ORDER BY fecha, id";
        try (Connection con = ConexionBD.abrir();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(crearDesdeFila(rs));
                }
            }
        }
        return lista;
    }

    /** Convierte la fila actual en un objeto MovimientoStock. */
    private static MovimientoStock crearDesdeFila(ResultSet rs) throws SQLException {
        return new MovimientoStock(
                rs.getLong("id"),
                rs.getString("producto_codigo"),
                MovimientoStock.TipoMovimiento.valueOf(rs.getString("tipo")),
                rs.getInt("cantidad"),
                rs.getTimestamp("fecha").toLocalDateTime(), // DATETIME -> LocalDateTime
                rs.getInt("stock_anterior"),
                rs.getInt("stock_posterior"));
    }
}
