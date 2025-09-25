Proyecto 2 de Teoria de Base de Datos 2
Nombre: Andrea Nicole Altamirano Tomé
Base de datos: Interbase
Realizado con Java y Swing para la parte grafica en NetBeans

Características principales
- Clase principal: Login.
- Permite la conexión a base de datos Interbase
- Permite la gestion de conexiones a las bases de datos ya que puede:
	- Agregar nuevas bases de datos
	- Desconectar bases de datos
	- Gestionar multiples conexiones de forma simultánea
	- Interactuar con ellas
- Interfaz para la ejecución de sentencias SQL sobre la base de datos conectada y elegida del JTree.
- Opciones para conectar, ejecutar SQL, guardar script, abrir script y crear tablas y views de manera visual.
- En el área de creación de tablas y vistas de manera visual, permite la creación automática del DDL, junto con un JTable cuando se crean tablas para visualizar cada columna que se agrega.
- Interfaz gráfica donde se ven los outputs de los queries realizados.
- Opción al darle doble clic sobre la base de datos, se muestra un submenú con las opciones de ver diagrama, sincronizar con Postgres y desconectar la base de datos.
- Opción al darle doble clic sobre la hoja llamada Tablas o Vistas, se muestra un submenú con la opción de ver diagrama.
- Opción al darle doble clic sobre una tabla o vista, se muestra un submenú con las opciones de ver diagrama, crear DDL y ver data.
- Permite la migración/sincronización de las tablas y vistas de Interbase a PostgreSQL al ingresar el host, nombre de la base de datos, user y contraseña.
- Manejo de bases de datos guardadas en un archivo JSON aun cuando cierra el programa para mantener la consistencia y persistencia de ellas mientras no sean cerradas.


Definición de clases
Conectar.java -> permite agregar nuevas bases de datos
Login -> Interfaz gráfica principal para escribir y ejecutar queries, manipular bases de datos, abrir/guardar scripts, crear tablas y vistas visualmente, y ver resultados.
Postgres -> Contiene la lógica para migrar/sincronizar tablas y vistas de Interbase a PostgreSQL.
createTV -> Permite la creación visual de tablas y vistas.
sincronizar -> Clase con método llamado desde Postgres para sincronizar tablas y vistas
ConexionIB -> permite la conexión a las bases de datos de Interbase
ConexionMng ->"Manager" principal de conexiones; funciones para abrir, cerrar y administrar conexiones.
ConexionPG -> Gestiona la conexión a PostgreSQL
Config -> Getters y setters para nombre, ruta, host, contraseña y user.
