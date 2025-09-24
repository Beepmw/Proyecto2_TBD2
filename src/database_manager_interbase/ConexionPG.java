/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database_manager_interbase;

/**
 *
 * @author tomea
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionPG {
    
    private Connection conn;
    
    public boolean conectarPG (String host, String db, String user, String contra){
        try{
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://" + host + ":5432/" + db;
            conn = DriverManager.getConnection(url,user,contra);
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    
    public Connection getConnection(){
        return conn;
    }
    
    
    public void cerrar(){
        try{
            if(conn !=null){
                conn.close();        
            }                
        }catch(SQLException e){
            e.printStackTrace();
        }
    }
          
    
    
}
