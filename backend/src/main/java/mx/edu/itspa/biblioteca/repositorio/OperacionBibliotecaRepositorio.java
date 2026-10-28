package mx.edu.itspa.biblioteca.repositorio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Acceso SQL al esquema JPA existente; no define reglas, estados ni políticas. */
@Repository
public class OperacionBibliotecaRepositorio {
    private final JdbcTemplate jdbc;
    public OperacionBibliotecaRepositorio(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Map<String,Object>> filas(String sql, Object... args) {
        return jdbc.query(sql, (rs,n) -> fila(rs), args);
    }
    public Optional<Map<String,Object>> primero(String sql, Object... args) {
        return filas(sql,args).stream().findFirst();
    }
    public long numero(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }
    public int ejecutar(String sql, Object... args) { return jdbc.update(sql,args); }
    public long insertar(String tabla, Map<String,Object> valores) {
        identificador(tabla); valores.keySet().forEach(this::identificador);
        String sql = "INSERT INTO " + tabla + " (" + String.join(",",valores.keySet()) + ") VALUES ("
            + String.join(",", Collections.nCopies(valores.size(),"?")) + ") RETURNING id";
        return Objects.requireNonNull(jdbc.queryForObject(sql,Long.class,valores.values().toArray()));
    }
    public void actualizar(String tabla,long id,Map<String,Object> valores) {
        identificador(tabla); valores.keySet().forEach(this::identificador);
        List<Object> args = new ArrayList<>(valores.values()); args.add(id);
        String set = String.join(",",valores.keySet().stream().map(k -> k + "=?").toList());
        jdbc.update("UPDATE " + tabla + " SET " + set + " WHERE id=?", args.toArray());
    }
    /** Serializa transiciones que afectan varias tablas; además se bloquean sus filas. */
    public void bloquearOperacion() { jdbc.query("SELECT pg_advisory_xact_lock(2942026)", rs -> { }); }
    private void identificador(String s) {
        if (!s.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Identificador SQL inválido");
    }
    private Map<String,Object> fila(ResultSet rs) throws SQLException {
        Map<String,Object> row = new LinkedHashMap<>();
        for(int i=1;i<=rs.getMetaData().getColumnCount();i++) {
            Object value=rs.getObject(i);
            if(value instanceof java.sql.Timestamp t) value=t.toLocalDateTime();
            else if(value instanceof java.sql.Date d) value=d.toLocalDate();
            else if(value instanceof java.sql.Time t) value=t.toLocalTime();
            row.put(rs.getMetaData().getColumnLabel(i),value);
        }
        return row;
    }
}
