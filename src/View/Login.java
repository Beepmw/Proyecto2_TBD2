/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package View;

import database_manager_interbase.*;
import interbase.interclient.*;
import java.awt.Graphics;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 *
 * @author tomea
 */
public class Login extends javax.swing.JFrame {

    /**
     * Creates new form login
     */
    private ConexionMng gestor;
    private JPopupMenu submenu;
    private JFrame frameDiagrama;
    private JPanel panelDiag;
    DefaultTableModel modelTable;

    public Login() {
        initComponents();
        gestor = new ConexionMng();
        modelTable = new DefaultTableModel();
        tbl_Result.setModel(modelTable);
        jTree1.setModel(null);
        cargarConexiones();
        actualizarArbol();
        setLocationRelativeTo(null);

    }

    public void agregarConexion(String nombre, Config cfg) {
        if (gestor.hacerConexion(nombre, cfg)) {
            actualizarArbol();
        }
    }

    public JTree getJTree() {
        return jTree1;
    }

    public void actualizarArbol() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Bases de Datos");

        for (String nombreConexion : gestor.conexiones.keySet()) {
            DefaultMutableTreeNode nodoBD = new DefaultMutableTreeNode(nombreConexion);

            try {
                Connection con = (Connection) gestor.getConexion(nombreConexion).getConnection();
                Statement st = (Statement) con.createStatement();

                DefaultMutableTreeNode nTablas = new DefaultMutableTreeNode("Tablas");
                ResultSet rs = (ResultSet) st.executeQuery(
                        "SELECT RDB$RELATION_NAME FROM RDB$RELATIONS WHERE RDB$SYSTEM_FLAG = 0 AND RDB$VIEW_BLR IS NULL ORDER BY RDB$RELATION_NAME");
                while (rs.next()) {
                    String tabla = rs.getString(1).trim();
                    DefaultMutableTreeNode nodoTabla = new DefaultMutableTreeNode(tabla);

                    PreparedStatement ps = (PreparedStatement) con.prepareStatement(
                            "SELECT rf.RDB$FIELD_NAME,f.RDB$FIELD_TYPE, f.RDB$FIELD_SUB_TYPE,f.RDB$FIELD_LENGTH, f.RDB$FIELD_PRECISION, f.RDB$FIELD_SCALE, rf.RDB$NULL_FLAG "
                            + "FROM RDB$RELATION_FIELDS rf JOIN RDB$FIELDS f ON rf.RDB$FIELD_SOURCE = f.RDB$FIELD_NAME WHERE rf.RDB$RELATION_NAME = ? ORDER BY rf.RDB$FIELD_POSITION");
                    ps.setString(1, tabla);
                    ResultSet cols = (ResultSet) ps.executeQuery();
                    while (cols.next()) {
                        String col = cols.getString("RDB$FIELD_NAME").trim();
                        int tipo = cols.getInt("RDB$FIELD_TYPE");
                        int subTipo = cols.getInt("RDB$FIELD_SUB_TYPE");
                        int precision = cols.getInt("RDB$FIELD_PRECISION");
                        int escala = cols.getInt("RDB$FIELD_SCALE");
                        boolean nnull = cols.getInt("RDB$NULL_FLAG") == 1;

                        String query = mapTipoInterBase(tipo, subTipo, precision, escala);
                        String columnaTree = col + " " + query;
                        if (nnull) {
                            columnaTree += " NOT NULL";
                        }
                        nodoTabla.add(new DefaultMutableTreeNode(columnaTree));
                    }
                    cols.close();
                    ps.close();
                    nTablas.add(nodoTabla);
                }
                rs.close();
                nodoBD.add(nTablas);

                DefaultMutableTreeNode nVistas = new DefaultMutableTreeNode("Vistas");
                rs = (ResultSet) st.executeQuery(
                        "SELECT RDB$RELATION_NAME FROM RDB$RELATIONS WHERE RDB$SYSTEM_FLAG = 0 AND RDB$VIEW_BLR IS NOT NULL ORDER BY RDB$RELATION_NAME");
                while (rs.next()) {
                    nVistas.add(new DefaultMutableTreeNode(rs.getString(1).trim()));
                }
                rs.close();
                nodoBD.add(nVistas);
                DefaultMutableTreeNode nProced = new DefaultMutableTreeNode("Procedimientos");
                try {
                    rs = (ResultSet) st.executeQuery(
                            "SELECT RDB$PROCEDURE_NAME FROM RDB$PROCEDURES WHERE COALESCE(RDB$SYSTEM_FLAG,0)=0 AND RDB$PACKAGE_NAME IS NULL ORDER BY 1");
                    while (rs.next()) {
                        nProced.add(new DefaultMutableTreeNode(rs.getString(1).trim()));
                    }
                    rs.close();
                } catch (SQLException e) {
                    nProced.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nProced);

                DefaultMutableTreeNode nFuncion = new DefaultMutableTreeNode("Funciones");
                try {
                    rs = (ResultSet) st.executeQuery(
                            "SELECT RDB$FUNCTION_NAME FROM RDB$FUNCTIONS WHERE COALESCE(RDB$SYSTEM_FLAG,0)=0 AND RDB$PACKAGE_NAME IS NULL ORDER BY 1");
                    while (rs.next()) {
                        nFuncion.add(new DefaultMutableTreeNode(rs.getString(1).trim()));
                    }
                    rs.close();
                } catch (SQLException e) {
                    nFuncion.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nFuncion);

                DefaultMutableTreeNode nPack = new DefaultMutableTreeNode("Paquetes");
                try {
                    rs = (ResultSet) st.executeQuery(
                            "SELECT RDB$PACKAGE_NAME FROM RDB$PACKAGES WHERE COALESCE(RDB$SYSTEM_FLAG,0)=0 ORDER BY 1");
                    while (rs.next()) {
                        String pack = rs.getString(1).trim();
                        DefaultMutableTreeNode nodoPkg = new DefaultMutableTreeNode(pack);

                        try (PreparedStatement ps = (PreparedStatement) con.prepareStatement(
                                "SELECT RDB$PROCEDURE_NAME FROM RDB$PROCEDURES WHERE RDB$PACKAGE_NAME=? ORDER BY 1")) {
                            ps.setString(1, pack);
                            ResultSet rs2 = (ResultSet) ps.executeQuery();
                            DefaultMutableTreeNode nProcs = new DefaultMutableTreeNode("Procedimientos");
                            while (rs2.next()) {
                                nProcs.add(new DefaultMutableTreeNode(rs2.getString(1).trim()));
                            }
                            rs2.close();
                            nodoPkg.add(nProcs);
                        }

                        try (PreparedStatement ps = (PreparedStatement) con.prepareStatement(
                                "SELECT RDB$FUNCTION_NAME FROM RDB$FUNCTIONS WHERE RDB$PACKAGE_NAME=? ORDER BY 1")) {
                            ps.setString(1, pack);
                            ResultSet rs2 = (ResultSet) ps.executeQuery();
                            DefaultMutableTreeNode nFuncs = new DefaultMutableTreeNode("Funciones");
                            while (rs2.next()) {
                                nFuncs.add(new DefaultMutableTreeNode(rs2.getString(1).trim()));
                            }
                            rs2.close();
                            nodoPkg.add(nFuncs);
                        }

                        nPack.add(nodoPkg);
                    }
                    rs.close();
                } catch (SQLException e) {
                    nPack.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nPack);

                DefaultMutableTreeNode nodoIndices = new DefaultMutableTreeNode("Índices");
                try {
                    rs = (ResultSet) st.executeQuery(
                            "SELECT i.RDB$INDEX_NAME, i.RDB$RELATION_NAME, i.RDB$UNIQUE_FLAG,i.RDB$INDEX_TYPE, i.RDB$INDEX_INACTIVE FROM RDB$INDICES i "
                            + "WHERE COALESCE(i.RDB$SYSTEM_FLAG,0)=0 ORDER BY i.RDB$RELATION_NAME, i.RDB$INDEX_NAME");
                    String currentTable = null;
                    DefaultMutableTreeNode nodoTablaIdx = null;
                    while (rs.next()) {
                        String tabla = rs.getString("RDB$RELATION_NAME").trim();
                        String i = rs.getString("RDB$INDEX_NAME").trim();

                        PreparedStatement ps = (PreparedStatement) con.prepareStatement(
                                "SELECT RDB$FIELD_NAME FROM RDB$INDEX_SEGMENTS "
                                + "WHERE RDB$INDEX_NAME=? ORDER BY RDB$FIELD_POSITION"
                        );
                        ps.setString(1, i);
                        ResultSet rsI = (ResultSet) ps.executeQuery();
                        java.util.List<String> cols = new java.util.ArrayList<>();
                        while (rsI.next()) {
                            cols.add(rsI.getString(1).trim());
                        }
                        rsI.close();
                        ps.close();

                        String label = i + " (" + String.join(", ", cols) + ")";
                        if (rs.getInt("RDB$UNIQUE_FLAG") == 1) {
                            label += " [UNIQUE]";
                        }
                        if (rs.getInt("RDB$INDEX_TYPE") == 1) {
                            label += " [DESC]";
                        }
                        if (rs.getInt("RDB$INDEX_INACTIVE") == 1) {
                            label += " [INACTIVE]";
                        }

                        if (!tabla.equals(currentTable)) {
                            nodoTablaIdx = new DefaultMutableTreeNode(tabla);
                            nodoIndices.add(nodoTablaIdx);
                            currentTable = tabla;
                        }
                        nodoTablaIdx.add(new DefaultMutableTreeNode(label));
                    }
                    rs.close();
                } catch (SQLException e) {
                    nodoIndices.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nodoIndices);

                DefaultMutableTreeNode nodoRoles = new DefaultMutableTreeNode("Roles");
                try {
                    rs = (ResultSet) st.executeQuery("SELECT RDB$ROLE_NAME FROM RDB$ROLES ORDER BY 1");
                    while (rs.next()) {
                        nodoRoles.add(new DefaultMutableTreeNode(rs.getString(1).trim()));
                    }
                    rs.close();
                } catch (SQLException e) {
                    nodoRoles.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nodoRoles);

                DefaultMutableTreeNode nodoUsuarios = new DefaultMutableTreeNode("Usuarios (con privilegios)");
                try {
                    rs = (ResultSet) st.executeQuery(
                            "SELECT DISTINCT RDB$USER "
                            + "FROM RDB$USER_PRIVILEGES "
                            + "WHERE RDB$USER_TYPE=8 ORDER BY 1"
                    );
                    while (rs.next()) {
                        nodoUsuarios.add(new DefaultMutableTreeNode(rs.getString(1).trim()));
                    }
                    rs.close();
                } catch (SQLException e) {
                    nodoUsuarios.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nodoUsuarios);

                DefaultMutableTreeNode nSecuencias = new DefaultMutableTreeNode("Secuencias");
                try {
                    rs = (ResultSet) st.executeQuery(
                            "SELECT RDB$GENERATOR_NAME FROM RDB$GENERATORS WHERE COALESCE(RDB$SYSTEM_FLAG,0)=0 ORDER BY 1"
                    );
                    while (rs.next()) {
                        nSecuencias.add(new DefaultMutableTreeNode(rs.getString(1).trim()));
                    }
                    rs.close();
                } catch (SQLException e) {
                    nSecuencias.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nSecuencias);

                DefaultMutableTreeNode nTriggers = new DefaultMutableTreeNode("Triggers");
                try {
                    rs = (ResultSet) st.executeQuery(
                            "SELECT RDB$TRIGGER_NAME FROM RDB$TRIGGERS WHERE COALESCE(RDB$SYSTEM_FLAG,0)=0 ORDER BY 1"
                    );
                    while (rs.next()) {
                        nTriggers.add(new DefaultMutableTreeNode(rs.getString(1).trim()));
                    }
                    rs.close();
                } catch (SQLException e) {
                    nTriggers.add(new DefaultMutableTreeNode("(no disponible)"));
                }
                nodoBD.add(nTriggers);

                st.close();
                con.close();

            } catch (SQLException e) {
                e.printStackTrace();
                nodoBD.add(new DefaultMutableTreeNode("(error al conectar)"));
            }

            root.add(nodoBD);
        }

        jTree1.setModel(new DefaultTreeModel(root));
        /*for (int i = 0; i < jTree1.getRowCount(); i++) {
        jTree1.expandRow(i);
    }*/
    }

    public ConexionMng getGestor() {
        return gestor;
    }

    public String mapTipoInterBase(int tipo, int subTipo, int precision, int scale) {
        switch (tipo) {
            case 7:
                return "SMALLINT";
            case 8:
                return "INTEGER";
            case 10:
                return "FLOAT";
            case 12:
                return "DATE";
            case 14:
                int prec = (precision <1) ? 255:precision;
                return "CHAR (" + prec + ")";
            case 16:
                if (scale == 0) {
                    return "BIGINT";
                } else {
                    return "NUMERIC(" + precision + "," + Math.abs(scale) + ")";
                }
            case 27:
                return "DOUBLE PRECISION";
            case 35:
                return "TIMESTAMP";
            case 37:
                int precc = (precision <1) ? 255:precision;
                return "VARCHAR(" + precc + ")";
            case 261:
                if (subTipo == 1) {
                    return "BLOB SUB_TYPE TEXT";
                } else {
                    return "BLOB";
                }
            default:
                return "UNKNOWN";
        }
    }

    private void cargarConexiones() {
        File file = new File("conexiones.json");
        if (!file.exists()) {
            return;
        }

        try {
            String contenido = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(contenido);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String nombre = obj.getString("nombre");
                String host = obj.getString("host");
                String ruta = obj.getString("ruta");
                String user = obj.getString("user");
                String pass = obj.getString("pass");

                if (!nombre.isEmpty() && !host.isEmpty() && !ruta.isEmpty()) {
                    Config cfg = new Config(nombre, host, ruta, user, pass);
                    gestor.hacerConexion(nombre, cfg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ejecutarSQL() throws SQLException {
        TreePath path = jTree1.getSelectionPath();
        if (path == null || path.getPathCount() < 2) {
            JOptionPane.showMessageDialog(this, "Selecciona una conexioooon en el arbol");
            return;
        }

        String nombreConexion = path.getPathComponent(1).toString();
        ConexionIB conexion = gestor.getConexion(nombreConexion);
        Config cfg = gestor.getConfig(nombreConexion);
        if (cfg == null) {
            JOptionPane.showMessageDialog(this, "No se hallo config para reconectar");
            return;
        }

        ConexionIB conexionNueva = new ConexionIB();
        boolean ok = conexionNueva.conectar(cfg);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo reconectar con la BD");
            return;
        }

        gestor.setConexion(nombreConexion, conexionNueva);
        conexion = conexionNueva;
        Connection con = (Connection) conexion.getConnection();
        String sql = txtQuery.getText().trim();
        System.out.println("SQL ejecutado: " + sql);

        if (sql.isEmpty()) {
            return;
        }

        try (Statement stmt = (Statement) con.createStatement()) {
            String sqlUpper = sql.trim().toUpperCase();

            if (sqlUpper.startsWith("SELECT")) {
                ResultSet rs = (ResultSet) stmt.executeQuery(sql);
                ResultSetMetaData meta = (ResultSetMetaData) rs.getMetaData();
                modelTable.setColumnCount(0);
                modelTable.setRowCount(0);

                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    modelTable.addColumn(meta.getColumnName(i));
                }

                while (rs.next()) {
                    Object[] fila = new Object[meta.getColumnCount()];
                    for (int i = 0; i < meta.getColumnCount(); i++) {
                        fila[i] = rs.getObject(i + 1);
                    }
                    modelTable.addRow(fila);
                }
                rs.close();

            } else if (sql.toLowerCase().startsWith("create")
                    || sql.toLowerCase().startsWith("alter")
                    || sql.toLowerCase().startsWith("drop")) {

                if (!sql.trim().endsWith(";")) {
                    sql = sql + ";";
                }

                stmt.executeUpdate(sql);
                JOptionPane.showMessageDialog(this, "Comando ejecutado correctamente.");
                actualizarArbol();
            } else {
                int afectados = stmt.executeUpdate(sql);

                if (!con.getAutoCommit()) {
                    con.commit();
                }

                JOptionPane.showMessageDialog(this, "Filas afectadas: " + afectados);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void exportar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar");
        chooser.setSelectedFile(new File("script.sql"));

        int opcion = chooser.showSaveDialog(this);
        if (opcion == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(chooser.getSelectedFile())) {
                fw.write(txtQuery.getText());
                JOptionPane.showMessageDialog(this, "Se exporto exitosamente");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error guardando el archivo: " + e.getMessage());
            }
        }
    }

    private void mostrarSubmenu(DefaultMutableTreeNode nodo, int x, int y) {
        if (submenu != null && submenu.isVisible()) {
            submenu.setVisible(false);
        }
        submenu = new JPopupMenu();

        if (nodo.getParent() != null && nodo.getParent().toString().equalsIgnoreCase("Bases de Datos")) {
            JMenuItem genDiagramDB = new JMenuItem("Generar Diagrama de Toda la Base");
            JMenuItem sync = new JMenuItem("Sincronizar con postgres ");
            String databaseName = nodo.getUserObject().toString();
     
            genDiagramDB.addActionListener(e -> verDiagrama(databaseName, null));
            
            sync.addActionListener(e -> {
                
                    Postgres pg = new Postgres(this,databaseName);
                    //sincronizarBD(databaseName);
                    pg.setVisible(true);
                
                
            });

            submenu.add(genDiagramDB);
            submenu.add(sync);  
        }

    

    else if (nodo.getUserObject () .toString().equalsIgnoreCase("Tablas") || nodo.getUserObject().toString().equalsIgnoreCase("Vistas")
        
            ) {
            JMenuItem genDiagramAllTables = new JMenuItem("Ver Diagrama");
            String databaseName = ((DefaultMutableTreeNode) nodo.getParent()).getUserObject().toString();
            genDiagramAllTables.addActionListener(e -> verDiagrama(databaseName, null));
            submenu.add(genDiagramAllTables);

        }else if (nodo.getParent() != null
                && (nodo.getParent().toString().equalsIgnoreCase("Tablas") || nodo.getParent().toString().equalsIgnoreCase("Vistas"))) {

            String tableName = nodo.getUserObject().toString();
            String dbName = ((DefaultMutableTreeNode) nodo.getParent().getParent()).getUserObject().toString();

            JMenuItem createDDL = new JMenuItem("Crear DDL");
            createDDL.addActionListener(e -> {
                try {
                    String ddl = mostrarDDL(tableName);
                    txtQuery.setText(ddl);
                    txtQuery.setCaretPosition(0);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                }
            });
            JMenuItem createDiagram = new JMenuItem("Ver Diagrama");
            createDiagram.addActionListener(e -> verDiagrama(dbName, tableName));

            JMenuItem viewData = new JMenuItem("Ver Data");
            viewData.addActionListener(e -> mostrarData(dbName, tableName));

            submenu.add(createDDL);
            submenu.add(createDiagram);
            submenu.add(viewData);
        }
        submenu.show(jTree1, x, y);
    }

    private String mostrarDDL(String tableName) throws SQLException {
        ConexionIB conexionIB = gestor.getConexion(seleccionarConexion());
        Connection con = (Connection) conexionIB.getConnection();
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(tableName).append(" (\n");

        PreparedStatement psCols = (PreparedStatement) con.prepareStatement(
                "SELECT rf.RDB$FIELD_NAME,f.RDB$FIELD_TYPE,f.RDB$FIELD_SUB_TYPE,f.RDB$FIELD_LENGTH,"
                + "f.RDB$FIELD_PRECISION,f.RDB$FIELD_SCALE,rf.RDB$NULL_FLAG "
                + "FROM RDB$RELATION_FIELDS rf JOIN RDB$FIELDS f ON rf.RDB$FIELD_SOURCE=f.RDB$FIELD_NAME "
                + "WHERE rf.RDB$RELATION_NAME=? ORDER BY rf.RDB$FIELD_POSITION");
        psCols.setString(1, tableName);
        ResultSet rsCols = (ResultSet) psCols.executeQuery();
        while (rsCols.next()) {
            String col = rsCols.getString("RDB$FIELD_NAME").trim();
            int tipo = rsCols.getInt("RDB$FIELD_TYPE");
            int subTipo = rsCols.getInt("RDB$FIELD_SUB_TYPE");
            int prec = rsCols.getInt("RDB$FIELD_PRECISION");
            int scale = rsCols.getInt("RDB$FIELD_SCALE");
            boolean notNull = rsCols.getInt("RDB$NULL_FLAG") == 1;
            ddl.append("  ").append(col).append(" ").append(mapTipoInterBase(tipo, subTipo, prec, scale));
            if (notNull) {
                ddl.append(" NOT NULL");
            }
            ddl.append(",\n");
        }
        rsCols.close();
        psCols.close();

        ddl.append(");");
        return ddl.toString();
    }

    private String seleccionarConexion() {
        TreePath path = jTree1.getSelectionPath();
        if (path != null && path.getPathCount() > 1) {
            return path.getPathComponent(1).toString();
        }
        return null;
    }

    private void mostrarData(String dbName, String tableName) {
        ConexionIB conexionIB = gestor.getConexion(dbName);
        if (conexionIB == null) {
            JOptionPane.showMessageDialog(this, "Error, no se conecta a la BD " + dbName);
            return;
        }
        try (Connection con = (Connection) conexionIB.getConnection(); Statement stmt = (Statement) con.createStatement()) {

            ResultSet rs = (ResultSet) stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData meta = (ResultSetMetaData) rs.getMetaData();
            modelTable.setColumnCount(0);
            modelTable.setRowCount(0);
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                modelTable.addColumn(meta.getColumnName(i).trim());
            }

            while (rs.next()) {
                Object[] fila = new Object[meta.getColumnCount()];
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    fila[i] = rs.getObject(i + 1);
                }
                modelTable.addRow(fila);
            }

            rs.close();
            tbl_Result.setModel(modelTable);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al obtener datos: " + ex.getMessage());
        }
    }

    private void verDiagrama(String DBname, String tableName) {
        try {
            ConexionIB conexionIB = gestor.getConexion(DBname);
            Connection con = (Connection) conexionIB.getConnection();

            mxGraph graph = new mxGraph();
            Object parent = graph.getDefaultParent();
            graph.getModel().beginUpdate();

            try {
                Map<String, Object> tblNodos = new HashMap<>();
                List<String> verTbl = new ArrayList<>();
                Map<String, Boolean> esVista = new HashMap<>();
                String sqlBase = "SELECT RDB$RELATION_NAME, RDB$VIEW_BLR FROM RDB$RELATIONS WHERE RDB$SYSTEM_FLAG=0";
                if (tableName != null) {
                    sqlBase += " AND RDB$RELATION_NAME='" + tableName.toUpperCase() + "'";
                }
                sqlBase += " ORDER BY RDB$RELATION_NAME";

                Statement st = (Statement) con.createStatement();
                ResultSet rsTbl = (ResultSet) st.executeQuery(sqlBase);

                while (rsTbl.next()) {
                    String nombreTbl = rsTbl.getString("RDB$RELATION_NAME").trim();
                    boolean vista = rsTbl.getObject("RDB$VIEW_BLR") != null;
                    verTbl.add(nombreTbl);
                    esVista.put(nombreTbl, vista);
                }
                rsTbl.close();

                if (tableName != null) {
                    String sqlRela
                            = "SELECT DISTINCT "
                            + "  CASE WHEN rc.RDB$RELATION_NAME = '" + tableName.toUpperCase() + "' THEN rc2.RDB$RELATION_NAME "
                            + "       ELSE rc.RDB$RELATION_NAME END AS TABLA_RELACIONADA "
                            + "FROM RDB$RELATION_CONSTRAINTS rc "
                            + "JOIN RDB$REF_CONSTRAINTS rfc ON rc.RDB$CONSTRAINT_NAME = rfc.RDB$CONSTRAINT_NAME "
                            + "JOIN RDB$RELATION_CONSTRAINTS rc2 ON rfc.RDB$CONST_NAME_UQ = rc2.RDB$CONSTRAINT_NAME "
                            + "WHERE (rc.RDB$RELATION_NAME = '" + tableName.toUpperCase() + "' OR rc2.RDB$RELATION_NAME = '" + tableName.toUpperCase() + "') "
                            + "AND rc.RDB$CONSTRAINT_TYPE = 'FOREIGN KEY'";

                    ResultSet rsRel = (ResultSet) st.executeQuery(sqlRela);
                    while (rsRel.next()) {
                        String tablaRel = rsRel.getString("TABLA_RELACIONADA");
                        if (tablaRel != null && !tablaRel.trim().equals(tableName.toUpperCase())
                                && !verTbl.contains(tablaRel.trim())) {
                            verTbl.add(tablaRel.trim());
                            esVista.put(tablaRel.trim(), false);
                        }
                    }
                    rsRel.close();
                }

                int maxCols = Math.max(3, (int) Math.ceil(Math.sqrt(verTbl.size())));
                int x = 50, y = 50, colActual = 0;

                for (String nombreTabla : verTbl) {
                    Set<String> primaryKeys = new HashSet<>();
                    String sqlPK
                            = "SELECT s.RDB$FIELD_NAME "
                            + "FROM RDB$RELATION_CONSTRAINTS rc "
                            + "JOIN RDB$INDEX_SEGMENTS s ON s.RDB$INDEX_NAME = rc.RDB$INDEX_NAME "
                            + "WHERE rc.RDB$RELATION_NAME = '" + nombreTabla + "' AND rc.RDB$CONSTRAINT_TYPE = 'PRIMARY KEY'";

                    ResultSet rsPK = (ResultSet) st.executeQuery(sqlPK);
                    while (rsPK.next()) {
                        primaryKeys.add(rsPK.getString("RDB$FIELD_NAME").trim());
                    }
                    rsPK.close();

                    Set<String> foreignKeys = new HashSet<>();
                    String sqlFK
                            = "SELECT s.RDB$FIELD_NAME "
                            + "FROM RDB$RELATION_CONSTRAINTS rc "
                            + "JOIN RDB$INDEX_SEGMENTS s ON s.RDB$INDEX_NAME = rc.RDB$INDEX_NAME "
                            + "WHERE rc.RDB$RELATION_NAME = '" + nombreTabla + "' AND rc.RDB$CONSTRAINT_TYPE = 'FOREIGN KEY'";

                    ResultSet rsFK = (ResultSet) st.executeQuery(sqlFK);
                    while (rsFK.next()) {
                        foreignKeys.add(rsFK.getString("RDB$FIELD_NAME").trim());
                    }
                    rsFK.close();

                    PreparedStatement psCol = (PreparedStatement) con.prepareStatement(
                            "SELECT rf.RDB$FIELD_NAME, f.RDB$FIELD_TYPE, f.RDB$FIELD_SUB_TYPE, "
                            + "f.RDB$FIELD_PRECISION, f.RDB$FIELD_SCALE, rf.RDB$NULL_FLAG "
                            + "FROM RDB$RELATION_FIELDS rf "
                            + "JOIN RDB$FIELDS f ON rf.RDB$FIELD_SOURCE = f.RDB$FIELD_NAME "
                            + "WHERE rf.RDB$RELATION_NAME = ? ORDER BY rf.RDB$FIELD_POSITION");
                    psCol.setString(1, nombreTabla);
                    ResultSet rsCols = (ResultSet) psCol.executeQuery();

                    StringBuilder contenido = new StringBuilder();
                    Boolean vista = esVista.get(nombreTabla);
                    String tipoTabla = (vista != null && vista) ? "Vista" : "Tabla";

                    contenido.append(nombreTabla).append(" (").append(tipoTabla).append(")\n");
                    contenido.append("==================\n");

                    int numColumnas = 0;
                    while (rsCols.next()) {
                        String colNombre = rsCols.getString("RDB$FIELD_NAME").trim();
                        int tipo = rsCols.getInt("RDB$FIELD_TYPE");
                        int subTipo = rsCols.getInt("RDB$FIELD_SUB_TYPE");
                        int precision = rsCols.getInt("RDB$FIELD_PRECISION");
                        int scale = rsCols.getInt("RDB$FIELD_SCALE");
                        boolean notNull = rsCols.getInt("RDB$NULL_FLAG") == 1;

                        String tipoStr = mapTipoInterBase(tipo, subTipo, precision, scale);
                        boolean esPK = primaryKeys.contains(colNombre);
                        boolean esFK = foreignKeys.contains(colNombre);

                        if (esPK) {
                            contenido.append("PK-> ").append(colNombre);
                        } else if (esFK) {
                            contenido.append("FK-> ").append(colNombre);
                        } else {
                            contenido.append("     ").append(colNombre);
                        }

                        contenido.append(" : ").append(tipoStr);

                        if (notNull && !esPK) {
                            contenido.append(" NN");
                        }

                        contenido.append("\n");
                        numColumnas++;
                    }

                    rsCols.close();
                    psCol.close();

                    int ancho = Math.max(250, Math.min(350, nombreTabla.length() * 15 + 80));
                    int alto = Math.max(120, 60 + (numColumnas * 20) + 15);

                    String estilo = "shape=rectangle;align=left;verticalAlign=top;spacingLeft=8;spacingTop=8;"
                            + "fontSize=11;fontFamily=monospaced;fillColor=#F0F8FF;strokeColor=#4682B4;"
                            + "strokeWidth=2;rounded=1;shadow=1;";

                    Object nodo = graph.insertVertex(parent, null, contenido.toString(), x, y, ancho, alto, estilo);
                    tblNodos.put(nombreTabla, nodo);

                    colActual++;
                    if (colActual >= maxCols) {
                        colActual = 0;
                        x = 50;
                        y += alto + 70;
                    } else {
                        x += ancho + 90;
                    }
                }

                String sqlRelaFK
                        = "SELECT rc.RDB$RELATION_NAME AS TABLA_ORIGEN, "
                        + "rc2.RDB$RELATION_NAME AS TABLA_REF, "
                        + "s.RDB$FIELD_NAME AS CAMPO_ORIGEN, "
                        + "s2.RDB$FIELD_NAME AS CAMPO_REF "
                        + "FROM RDB$RELATION_CONSTRAINTS rc "
                        + "JOIN RDB$REF_CONSTRAINTS rfc ON rc.RDB$CONSTRAINT_NAME = rfc.RDB$CONSTRAINT_NAME "
                        + "JOIN RDB$RELATION_CONSTRAINTS rc2 ON rfc.RDB$CONST_NAME_UQ = rc2.RDB$CONSTRAINT_NAME "
                        + "JOIN RDB$INDEX_SEGMENTS s ON s.RDB$INDEX_NAME = rc.RDB$INDEX_NAME "
                        + "JOIN RDB$INDEX_SEGMENTS s2 ON s2.RDB$INDEX_NAME = rc2.RDB$INDEX_NAME "
                        + "WHERE rc.RDB$CONSTRAINT_TYPE = 'FOREIGN KEY'";

                ResultSet rsRelFK = (ResultSet) st.executeQuery(sqlRelaFK);
                while (rsRelFK.next()) {
                    String origen = rsRelFK.getString("TABLA_ORIGEN").trim();
                    String destino = rsRelFK.getString("TABLA_REF").trim();
                    String campoOrigen = rsRelFK.getString("CAMPO_ORIGEN").trim();
                    String campoRef = rsRelFK.getString("CAMPO_REF").trim();

                    Object nodoOrigen = tblNodos.get(origen);
                    Object nodoDestino = tblNodos.get(destino);

                    if (nodoOrigen != null && nodoDestino != null) {
                        String etiqueta = campoOrigen.equals(campoRef) ? campoOrigen : campoOrigen + "->" + campoRef;

                        graph.insertEdge(parent, null, etiqueta, nodoOrigen, nodoDestino,
                                "strokeColor=#4682B4;strokeWidth=2;endArrow=block;endFill=1;"
                                + "fontSize=10;fontColor=#2F4F4F;labelBackgroundColor=#FFFFFF;"
                                + "edgeStyle=orthogonalEdgeStyle;rounded=1;");
                    }
                }
                rsRelFK.close();
                st.close();

            } finally {
                graph.getModel().endUpdate();
            }

            mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
            layout.setIntraCellSpacing(80);
            layout.setInterRankCellSpacing(130);
            layout.setInterHierarchySpacing(160);
            layout.setFineTuning(true);
            layout.execute(graph.getDefaultParent());

            mxGraphComponent graphComponent = new mxGraphComponent(graph);
            graphComponent.setConnectable(false);
            graphComponent.setDragEnabled(true);
            graphComponent.setPanning(true);
            graphComponent.setKeepSelectionVisibleOnZoom(true);
            graphComponent.setAntiAlias(true);
            graphComponent.getViewport().setOpaque(true);
            graphComponent.getViewport().setBackground(java.awt.Color.WHITE);

            String titulo = tableName != null
                    ? "Diagrama ER - Tabla: " + tableName + " y Relaciones"
                    : "Diagrama ER - Base de Datos: " + DBname;

            JFrame frame = new JFrame(titulo);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(new JScrollPane(graphComponent));
            frame.setSize(1300, 850);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar diagrama: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
    
    private void sincronizarBD(String nombreConexionInterbase) throws SQLException {
    String hostPG = "localhost";
    String dbPG = "postgres";
    String userPG = "postgres";
    String passPG = "postgres";

    ConexionIB interbase = gestor.getConexion(nombreConexionInterbase);
    ConexionPG postgres = new ConexionPG();
    if (!postgres.conectarPG(hostPG, dbPG, userPG, passPG)) {
        JOptionPane.showMessageDialog(this, "No se pudo conectar a PostgreSQL");
        return;
    }

    sincronizar sync = new sincronizar(interbase.getConnection(), (java.sql.Connection) postgres.getConnection());

    try {

        sync.syncTblRel(); 
        //sync.syncAllViews();
        JOptionPane.showMessageDialog(this, "Sincronización completada para " + nombreConexionInterbase + ".");
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Ocurrió un error: " + ex.getMessage());
        ex.printStackTrace();
    } finally {
        postgres.cerrar();
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        btn_newServer = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtQuery = new javax.swing.JTextArea();
        btnRunSql = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tbl_Result = new javax.swing.JTable();
        btnExportar = new javax.swing.JButton();
        btnCreate = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(204, 204, 204));

        jPanel1.setBackground(new java.awt.Color(102, 204, 255));

        jTree1.setBackground(new java.awt.Color(255, 255, 255));
        jTree1.setFont(new java.awt.Font("Nirmala UI", 0, 12)); // NOI18N
        jTree1.setForeground(new java.awt.Color(0, 0, 0));
        jTree1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTree1MouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTree1MousePressed(evt);
            }
        });
        jScrollPane1.setViewportView(jTree1);

        btn_newServer.setBackground(new java.awt.Color(255, 255, 255));
        btn_newServer.setFont(new java.awt.Font("Nirmala UI", 0, 12)); // NOI18N
        btn_newServer.setForeground(new java.awt.Color(0, 0, 0));
        btn_newServer.setText("Agregar base de datos");
        btn_newServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_newServerActionPerformed(evt);
            }
        });

        txtQuery.setBackground(new java.awt.Color(255, 255, 255));
        txtQuery.setColumns(20);
        txtQuery.setFont(new java.awt.Font("Nirmala UI", 0, 12)); // NOI18N
        txtQuery.setForeground(new java.awt.Color(0, 0, 0));
        txtQuery.setRows(5);
        jScrollPane2.setViewportView(txtQuery);

        btnRunSql.setBackground(new java.awt.Color(255, 255, 255));
        btnRunSql.setFont(new java.awt.Font("Nirmala UI", 0, 12)); // NOI18N
        btnRunSql.setForeground(new java.awt.Color(0, 0, 0));
        btnRunSql.setText("Run");
        btnRunSql.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRunSqlActionPerformed(evt);
            }
        });

        tbl_Result.setBackground(new java.awt.Color(255, 255, 255));
        tbl_Result.setFont(new java.awt.Font("Nirmala UI", 0, 12)); // NOI18N
        tbl_Result.setForeground(new java.awt.Color(0, 0, 0));
        tbl_Result.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tbl_Result.setEnabled(false);
        jScrollPane3.setViewportView(tbl_Result);

        btnExportar.setBackground(new java.awt.Color(255, 255, 255));
        btnExportar.setFont(new java.awt.Font("Nirmala UI", 0, 12)); // NOI18N
        btnExportar.setForeground(new java.awt.Color(0, 0, 0));
        btnExportar.setText("Exportar");
        btnExportar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportarActionPerformed(evt);
            }
        });

        btnCreate.setBackground(new java.awt.Color(255, 255, 255));
        btnCreate.setFont(new java.awt.Font("Nirmala UI", 0, 12)); // NOI18N
        btnCreate.setForeground(new java.awt.Color(0, 0, 0));
        btnCreate.setText("Create");
        btnCreate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(18, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 537, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 537, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(60, 60, 60))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(btn_newServer)
                .addGap(72, 72, 72)
                .addComponent(btnRunSql)
                .addGap(26, 26, 26)
                .addComponent(btnExportar)
                .addGap(26, 26, 26)
                .addComponent(btnCreate)
                .addContainerGap(294, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(49, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_newServer)
                    .addComponent(btnRunSql)
                    .addComponent(btnExportar)
                    .addComponent(btnCreate))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 397, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(43, 43, 43))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn_newServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_newServerActionPerformed

        Conectar con = new Conectar(this, this.gestor);
        con.setVisible(true);


    }//GEN-LAST:event_btn_newServerActionPerformed

    private void btnRunSqlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRunSqlActionPerformed
        try {
            ejecutarSQL();
        } catch (SQLException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnRunSqlActionPerformed

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarActionPerformed
        exportar();
    }//GEN-LAST:event_btnExportarActionPerformed

    private void btnCreateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateActionPerformed
        createTV tv = new createTV(this);
        tv.setVisible(true);
    }//GEN-LAST:event_btnCreateActionPerformed

    private void jTree1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree1MouseClicked
        if (evt.getClickCount() == 2) {
            TreePath ruta = jTree1.getPathForLocation(evt.getX(), evt.getY());
            if (ruta != null) {
                DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) ruta.getLastPathComponent();
                mostrarSubmenu(nodo, evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_jTree1MouseClicked

    private void jTree1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree1MousePressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTree1MousePressed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Login().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCreate;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnRunSql;
    private javax.swing.JButton btn_newServer;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTree jTree1;
    private javax.swing.JTable tbl_Result;
    private javax.swing.JTextArea txtQuery;
    // End of variables declaration//GEN-END:variables

}
