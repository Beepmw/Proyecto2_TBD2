/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database_manager_interbase;

import java.sql.*;

/**
 *
 * @author tomea
 */
public class ConexionIB {

    private Connection conect;
    private Config conf;

    public boolean conectar(Config conf) {
        this.conf = conf;
        try {
            Class.forName("interbase.interclient.Driver");
            String url = "jdbc:interbase://" + conf.getHost() + "/" + conf.getRuta() + "?enableProtocol=0";
            System.out.println(url);
            conect = DriverManager.getConnection(url, conf.getUser(), conf.getContra());
            System.out.println("Conexion exitosa a:" + conf.getRuta());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Driver no encontrado");
            conect = null;
            System.out.println("Conexion fallida");
            return false;
        }
    }

    public Connection getConnection() throws SQLException {
        if (conect == null || conect.isClosed()) {
            abrir();
        }
        return conect;
    }

    public void cerrar() {
        if (conect != null) {
            try {
                conect.close();
            } catch (SQLException ignore) {
            }
            conect = null;
        }
    }

    public boolean estaAbierta() {
        try {
            return conect != null && !conect.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void abrir() throws SQLException {
        if (conf != null) {
            try {
                Class.forName("interbase.interclient.Driver");
                String url = "jdbc:interbase://" + conf.getHost() + "/" + conf.getRuta() + "?enableProtocol=0";
                conect = DriverManager.getConnection(url, conf.getUser(), conf.getContra());
                System.out.println("Conexión abierta" + conf.getRuta());
            } catch (Exception e) {
                e.printStackTrace();
                throw new SQLException("No se pudo abrir");
            }
        } else {
            throw new SQLException("Error, ya no se puede abrir");
        }
    }
}
