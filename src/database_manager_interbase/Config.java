/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database_manager_interbase;

/**
 *
 * @author tomea
 */
public class Config {
    private String nombre;   
    private String host;
    private String ruta;     
    private String user;
    private String contra;

    public Config(String nombre, String host, String ruta, String user, String contra) {
        this.nombre = nombre;
        this.host = host;
        this.ruta = ruta;
        this.user = user;
        this.contra = contra;
    }

    public String getNombre() {
        return nombre;
    }

    public String getHost() {
        return host;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setContra(String contra) {
        this.contra = contra;
    }


    public String getRuta() {
        return ruta;
    }

    public String getUser() {
        return user;
    }

    public String getContra() {
        return contra;
    }

   /* @Override
    public String toString() {
        return nombre+"("+host+":"+puerto+")"; 
    }
*/
    
    
          
    
}