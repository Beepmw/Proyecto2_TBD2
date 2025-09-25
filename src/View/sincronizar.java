/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package View;

/**
 *
 * @author tomea
 */
import java.sql.*;
import java.util.*;

public class sincronizar {

    private Connection IBconn;
    private Connection PGconn;

    public sincronizar(Connection IBconn, Connection PGconn) {
        this.IBconn = IBconn;
        this.PGconn = PGconn;
    }

    private String tipoDatoPG(int tipo, int subtipo, int precision, int scale) {
        switch (tipo) {
            case 7:
                return "SMALLINT";
            case 8:
                return "INTEGER";
            case 10:
                return "REAL";
            case 12:
                return "DATE";
            case 14:
                return "CHAR";
            case 16:
                return (scale == 0) ? "BIGINT" : "NUMERIC(" + precision + "," + Math.abs(scale) + ")";
            case 27:
                return "DBLE PRECISION";
            case 35:
                return "TIMESTAMP";
            case 37:
                int prec = (precision < 1) ? 255 : precision;
                return "VARCHAR(" + prec + ")";
            case 261:
                return "BYTEA";
            default:
                return "TEXT";
        }

    }

    public void syncTblRel() throws SQLException {

        Statement stTablas = IBconn.createStatement();
        ResultSet rsTablas = stTablas.executeQuery(
                "SELECT RDB$RELATION_NAME FROM RDB$RELATIONS "
                + "WHERE RDB$SYSTEM_FLAG = 0 AND RDB$VIEW_BLR IS NULL "
                + "ORDER BY RDB$RELATION_NAME");

        while (rsTablas.next()) {
            String tabla = rsTablas.getString(1).trim();
            System.out.println("Sincronizando tabla: " + tabla);
            try (Statement stmtpg = PGconn.createStatement()) {
                stmtpg.executeUpdate("DROP TABLE IF EXISTS " + tabla + " CASCADE");
                System.out.println("tabla eliminada " + tabla);
            } catch (SQLException e) {
                System.err.println("error eliminando la tabla " + tabla + e.getMessage());
            }

            syncTodo(tabla);
        }
        rsTablas.close();
        stTablas.close();
    }

    public void syncTodo(String tableName) throws SQLException {
        PreparedStatement psCols = IBconn.prepareStatement("SELECT rf.RDB$FIELD_NAME, f.RDB$FIELD_TYPE, f.RDB$FIELD_SUB_TYPE, f.RDB$FIELD_PRECISION, "
                + "f.RDB$FIELD_SCALE, rf.RDB$NULL_FLAG "
                + "FROM RDB$RELATION_FIELDS rf JOIN RDB$FIELDS f ON rf.RDB$FIELD_SOURCE=f.RDB$FIELD_NAME "
                + "WHERE rf.RDB$RELATION_NAME=? ORDER BY rf.RDB$FIELD_POSITION");
        psCols.setString(1, tableName);
        ResultSet rsCols = psCols.executeQuery();

        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        List<String> colNames = new ArrayList<>();
        while (rsCols.next()) {
            String col = rsCols.getString("RDB$FIELD_NAME").trim();
            int tipo = rsCols.getInt("RDB$FIELD_TYPE");
            int subTipo = rsCols.getInt("RDB$FIELD_SUB_TYPE");
            int prec = rsCols.getInt("RDB$FIELD_PRECISION");
            int scale = rsCols.getInt("RDB$FIELD_SCALE");
            boolean notNull = rsCols.getInt("RDB$NULL_FLAG") == 1;
            ddl.append(col).append(" ").append(tipoDatoPG(tipo, subTipo, prec, scale));
            if (notNull) {
                ddl.append(" NOT NULL");
            }
            ddl.append(", ");
            colNames.add(col);
        }
        rsCols.close();
        psCols.close();

        if (colNames.size() == 0) {
            return;
        }

// hallo PK
        List<String> pkFields = new ArrayList<>();
        Statement stPK = IBconn.createStatement();
        ResultSet rsPK = stPK.executeQuery(
                "SELECT s.RDB$FIELD_NAME FROM RDB$RELATION_CONSTRAINTS rc "
                + "JOIN RDB$INDEX_SEGMENTS s ON s.RDB$INDEX_NAME = rc.RDB$INDEX_NAME "
                + "WHERE rc.RDB$RELATION_NAME = '" + tableName + "' AND rc.RDB$CONSTRAINT_TYPE = 'PRIMARY KEY'");
        while (rsPK.next()) {
            pkFields.add(rsPK.getString(1).trim());
        }
        rsPK.close();
        stPK.close();

        if (!pkFields.isEmpty()) {
            ddl.append("PRIMARY KEY (").append(String.join(",", pkFields)).append("), ");
        }

//hallo FK
        Statement stmtfk = IBconn.createStatement();
        ResultSet rsFK = stmtfk.executeQuery(
                "SELECT rc.RDB$CONSTRAINT_NAME, s.RDB$FIELD_NAME, ref.RDB$RELATION_NAME as ref_table, seg.RDB$FIELD_NAME as ref_field "
                + "FROM RDB$RELATION_CONSTRAINTS rc "
                + "JOIN RDB$INDEX_SEGMENTS s ON s.RDB$INDEX_NAME = rc.RDB$INDEX_NAME "
                + "JOIN RDB$REF_CONSTRAINTS r ON rc.RDB$CONSTRAINT_NAME = r.RDB$CONSTRAINT_NAME "
                + "JOIN RDB$RELATION_CONSTRAINTS ref ON r.RDB$CONST_NAME_UQ = ref.RDB$CONSTRAINT_NAME "
                + "JOIN RDB$INDEX_SEGMENTS seg ON seg.RDB$INDEX_NAME = ref.RDB$INDEX_NAME "
                + "WHERE rc.RDB$RELATION_NAME = '" + tableName + "' AND rc.RDB$CONSTRAINT_TYPE = 'FOREIGN KEY'");
        while (rsFK.next()) {
            String fkName = rsFK.getString("RDB$CONSTRAINT_NAME").trim();
            String col = rsFK.getString("RDB$FIELD_NAME").trim();
            String refTable = rsFK.getString("ref_table").trim();
            String refField = rsFK.getString("ref_field").trim();
            ddl.append("CONSTRAINT ").append(fkName).append(" FOREIGN KEY (").append(col)
                    .append(") REFERENCES ").append(refTable).append("(").append(refField).append("), ");
        }
        rsFK.close();
        stmtfk.close();
        ddl.setLength(ddl.length() - 2);
        ddl.append(");");

        Statement stmtpg = PGconn.createStatement();
        stmtpg.execute(ddl.toString());

        if (pkFields.isEmpty()) {
            System.out.println("Tabla " + tableName + " sin PK - Limpiando tabla completa");
            stmtpg.execute("DELETE FROM " + tableName);
        } else {
            System.out.println("Tabla " + tableName + " con PK - Sincronización incremental");
        }

        Statement stData = IBconn.createStatement();
        ResultSet rsData = stData.executeQuery("SELECT * FROM " + tableName);
        ResultSetMetaData meta = rsData.getMetaData();

        Set<String> pkValuesInterbase = new HashSet<>();

        while (rsData.next()) {
            StringBuilder sbInsert = new StringBuilder();
            sbInsert.append("INSERT INTO ").append(tableName).append(" (")
                    .append(String.join(", ", colNames)).append(") VALUES (");

            for (int i = 1; i <= meta.getColumnCount(); i++) {
                Object val = rsData.getObject(i);
                if (val == null) {
                    sbInsert.append("NULL");
                } else {
                    sbInsert.append("'").append(val.toString().replace("'", "''")).append("'");
                }
                if (i < meta.getColumnCount()) {
                    sbInsert.append(", ");
                }
            }
            sbInsert.append(")");

            if (!pkFields.isEmpty()) {
                sbInsert.append(" ON CONFLICT (").append(String.join(", ", pkFields)).append(") DO UPDATE SET ");
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String colName = meta.getColumnName(i);
                    sbInsert.append(colName).append("=EXCLUDED.").append(colName);
                    if (i < meta.getColumnCount()) {
                        sbInsert.append(", ");
                    }
                }
            }
            sbInsert.append(";");

            if (!pkFields.isEmpty()) {
                StringBuilder pkVal = new StringBuilder();
                for (String pk : pkFields) {
                    pkVal.append(rsData.getString(pk)).append("|");
                }
                pkValuesInterbase.add(pkVal.toString());
            }

            try {
                stmtpg.execute(sbInsert.toString());
            } catch (SQLException ex) {
                System.out.println("Error insertando fila: " + ex.getMessage());
            }
        }
        rsData.close();
        stData.close();

        if (!pkFields.isEmpty() && !pkValuesInterbase.isEmpty()) {
            StringBuilder sqlSelectPG = new StringBuilder();
            sqlSelectPG.append("SELECT ").append(String.join(", ", pkFields)).append(" FROM ").append(tableName);
            Statement stPGSelect = PGconn.createStatement();
            ResultSet rsPG = stPGSelect.executeQuery(sqlSelectPG.toString());
            List<String> pkValuesPostgres = new ArrayList<>();
            while (rsPG.next()) {
                StringBuilder pkVal = new StringBuilder();
                for (String pk : pkFields) {
                    pkVal.append(rsPG.getString(pk)).append("|");
                }
                pkValuesPostgres.add(pkVal.toString());
            }
            rsPG.close();
            stPGSelect.close();

            for (String pkPostgres : pkValuesPostgres) {
                if (!pkValuesInterbase.contains(pkPostgres)) {
                    String[] pkParts = pkPostgres.split("\\|");
                    StringBuilder where = new StringBuilder();
                    for (int i = 0; i < pkFields.size(); i++) {
                        where.append(pkFields.get(i)).append("='").append(pkParts[i]).append("'");
                        if (i < pkFields.size() - 1) {
                            where.append(" AND ");
                        }
                    }
                    String sqlDelete = "DELETE FROM " + tableName + " WHERE " + where;
                    try {
                        stmtpg.execute(sqlDelete);
                    } catch (SQLException ex) {
                        System.out.println("Error borrando fila: " + ex.getMessage());
                    }
                }
            }
        }
        stmtpg.close();
    }

    public void syncTVista() throws SQLException {
        Statement stVistas = IBconn.createStatement();
        ResultSet rsVistas = stVistas.executeQuery(
                "SELECT RDB$RELATION_NAME FROM RDB$RELATIONS "
                + "WHERE RDB$SYSTEM_FLAG = 0 AND RDB$VIEW_BLR IS NOT NULL "
                + "ORDER BY RDB$RELATION_NAME");

        while (rsVistas.next()) {
            String vista = rsVistas.getString(1).trim();
            System.out.println("Migrando vista: " + vista);
            syncView(vista);
        }
        rsVistas.close();
        stVistas.close();
    }

    public void syncView(String viewName) throws SQLException {
        String sqlDef = null;
        PreparedStatement ps = IBconn.prepareStatement(
                "SELECT rdb$view_source FROM rdb$relations WHERE rdb$relation_name = ?");
        ps.setString(1, viewName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            sqlDef = rs.getString(1);
        }
        rs.close();
        ps.close();

        if (sqlDef != null && !sqlDef.isEmpty()) {
            // Limpieza básica para que Postgres lo acepte
            sqlDef = sqlDef.trim()
                    .replaceAll("\\s+", " "); 

            String ddl = "CREATE OR REPLACE VIEW " + viewName + " AS " + sqlDef;

            Statement stPg = PGconn.createStatement();
            try {
                stPg.execute(ddl);
                System.out.println("Vista creada/migrada en Postgres: " + viewName);
            } catch (SQLException ex) {
                System.out.println("Error migrando vista " + viewName + ": " + ex.getMessage());
            }
            stPg.close();
        }
    }

}
