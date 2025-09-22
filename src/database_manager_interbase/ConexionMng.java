/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database_manager_interbase;

import java.util.HashMap;
import java.util.Map;

public class ConexionMng {

    public Map<String, ConexionIB> conexiones = new HashMap<>();
    private Map<String, Config> configs = new HashMap<>();

    public boolean hacerConexion(String nombre, Config cfg) {
        if(conexiones.containsKey(nombre)) {
            System.out.println("Ya existe una conexión con ese nombre");
            return false;
        }

        ConexionIB conexion = new ConexionIB();
        boolean listo = conexion.conectar(cfg);

        if(listo){
            conexiones.put(nombre, conexion);
            configs.put(nombre, cfg);
            return true;
        }
        return false;
    }
    
    public ConexionIB getConexion(String nombre) {
        return conexiones.get(nombre);
    }

    public Config getConfig(String nombre) {
        return configs.get(nombre);
    }

    public int cantidadConexiones() {
        return conexiones.size();
    }

    public void cerrarTodas() {
        for (ConexionIB c : conexiones.values()) {
            try { c.cerrar(); } catch(Exception e) { e.printStackTrace(); }
        }
        conexiones.clear();
        configs.clear();
    }

    public void cerrarConexion(String nombre) {
        if(conexiones.containsKey(nombre)){
            try { conexiones.get(nombre).cerrar(); } catch(Exception e) { e.printStackTrace(); }
            conexiones.remove(nombre);
            configs.remove(nombre);
        }
    }
    public void setConexion(String nombre, ConexionIB conexion) {
    conexiones.put(nombre, conexion);
}
}